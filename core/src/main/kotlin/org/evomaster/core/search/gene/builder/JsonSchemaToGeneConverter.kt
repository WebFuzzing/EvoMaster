package org.evomaster.core.search.gene.builder

import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.EMConfig
import org.evomaster.core.StaticCounter
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.ObjectWithAttributesGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.collection.TaintedMapGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.FormatForDatesAndTimes
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.numeric.NumberGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.LimitObjectGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.wrapper.ChoiceGene
import org.evomaster.core.search.gene.wrapper.NullableGene
import org.evomaster.core.search.gene.wrapper.OptionalGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayDeque
import java.util.Deque
import kotlin.math.max

/**
 * Protocol-agnostic JSON Schema → [Gene] pipeline.
 *
 * Originally lived inside `RestActionBuilderV3` and was tightly coupled to
 * `RestSchema`/`SchemaOpenAPI` for `$ref` resolution.  That coupling forced
 * AsyncAPI to synthesise a fake OpenAPI document just to call the same
 * conversion logic.  Lifting the pipeline here, behind a [SchemaRefResolver],
 * lets every caller (REST, AsyncAPI, RPC's runtime DTO bridge in the future)
 * share the same JSON-Schema-aware logic without inventing protocol-specific
 * shims.
 *
 * The instance holds a per-converter `$ref` cache so root-mounted gene trees
 * are reused — the cache is keyed by the `$ref` string and is therefore
 * invalidated automatically when callers create a fresh converter (which
 * happens once per top-level build).
 *
 * This class is the new home for what used to be:
 *  - `RestActionBuilderV3.getGene` and friends,
 *  - `RestActionBuilderV3.createObjectFromReference`,
 *  - object/oneOf/allOf/anyOf assembly,
 *  - the constraint-aware non-object gene builder,
 *  - example/default handling.
 */
class JsonSchemaToGeneConverter(
    private val refResolver: SchemaRefResolver,
    private val options: Options
) {

    /**
     * Conversion knobs.  Mirrors the old `RestActionBuilderV3.Options` —
     * relocated here so callers do not need to import REST types just to
     * configure the converter.
     */
    class Options(
        @Deprecated("No longer maintained")
        val doParseDescription: Boolean = false,
        val enableConstraintHandling: Boolean = true,
        val invalidData: Boolean = false,
        val probUseDefault: Double = 0.0,
        val probUseExamples: Double = 0.0,
        val usingWhiteBox: Boolean = true
    ) {
        constructor(config: EMConfig) : this(
            enableConstraintHandling = config.enableSchemaConstraintHandling,
            invalidData = config.allowInvalidData,
            probUseDefault = config.probRestDefault,
            probUseExamples = config.probRestExamples,
            usingWhiteBox = !config.blackBox
        )

        init {
            if (probUseDefault < 0 || probUseDefault > 1) {
                throw IllegalArgumentException("Invalid probUseDefault: $probUseDefault")
            }
            if (probUseExamples < 0 || probUseExamples > 1) {
                throw IllegalArgumentException("Invalid probUseExamples: $probUseExamples")
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JsonSchemaToGeneConverter::class.java)

        const val EXAMPLES_NAME: String = "SCHEMA_EXAMPLES"
    }

    private val refCache = mutableMapOf<String, Gene>()

    /** Drop the cached gene trees.  Useful between unrelated builds. */
    fun cleanCache() {
        refCache.clear()
    }

    /**
     * Top-level conversion entry point.  Walks [schema] and produces a
     * [Gene] tree.  When [schema] is a `$ref` the resolver is consulted;
     * cycles and depth limits are guarded internally.
     */
    fun getGene(
        name: String,
        schema: Schema<*>,
        history: Deque<String> = ArrayDeque(),
        referenceClassDef: String? = null,
        isInPath: Boolean = false,
        examples: List<Pair<Any, String?>> = listOf(),
        messages: MutableList<String>
    ): Gene {

        if (!schema.`$ref`.isNullOrBlank()) {
            return createObjectFromReference(name, schema.`$ref`, history, examples, messages)
        }

        val nullable30 = schema.nullable != null && schema.nullable
        val nullable31 = schema.types?.contains("null") ?: false
        val nullable = nullable30 || nullable31

        val gene = getNonNullGene(
            schema,
            name,
            messages,
            isInPath,
            examples,
            history,
            referenceClassDef
        )

        return possiblyNullable(gene, nullable)
    }

    fun possiblyNullable(gene: Gene, nullable: Boolean): Gene {
        if (nullable) {
            return NullableGene(gene.name, gene).also { GeneUtils.preventCycles(it) }
        }
        return gene
    }

    fun possiblyOptional(gene: Gene, required: Boolean?): Gene {
        if (required != true) {
            return OptionalGene(gene.name, gene).also { GeneUtils.preventCycles(it) }
        }
        return gene
    }

    /**
     * Resolves a `$ref` and recursively builds the gene tree for the target
     * schema.  Caches root-mounted trees by `$ref` string; tracks depth and
     * cycles via [history].
     */
    fun createObjectFromReference(
        name: String,
        reference: String,
        history: Deque<String> = ArrayDeque(),
        examples: List<Pair<Any, String?>> = listOf(),
        messages: MutableList<String>
    ): Gene {

        val isRoot = history.isEmpty()
        val cycleDepth = 1

        if (history.count { it == reference } >= cycleDepth) {
            return CycleObjectGene("Cycle for: $reference")
        }

        if (isRoot && refCache.containsKey(reference)) {
            val copy = refCache[reference]!!.copy()
            copy.name = name
            return copy
        }

        val depthLimit = 5
        if (history.size == depthLimit) {
            return LimitObjectGene("Object-depth limit reached for: $reference")
        }

        try {
            URI(reference)
        } catch (e: URISyntaxException) {
            LoggingUtil.uniqueWarn(log, "Object reference is not a valid URI: $reference")
        }

        val schema = refResolver.resolve(reference, messages)

        if (schema == null) {
            val classDef = getClassDef(reference)
            LoggingUtil.uniqueWarn(log, "No $classDef among the schema definitions reachable from this resolver")
            return ObjectGene(name, listOf(), classDef)
        }

        history.push(reference)
        val gene = getGene(
            name,
            schema,
            history,
            referenceClassDef = getClassDef(reference),
            examples = examples,
            messages = messages
        )

        if (isRoot) {
            GeneUtils.preventCycles(gene)
            GeneUtils.preventLimit(gene)
            refCache[reference] = gene
        }

        history.pop()
        return gene
    }

    private fun getNonNullGene(
        schema: Schema<*>,
        name: String,
        messages: MutableList<String>,
        isInPath: Boolean,
        examples: List<Pair<Any, String?>>,
        history: Deque<String>,
        referenceClassDef: String?
    ): Gene {

        val type = schema.type ?: schema.types?.firstOrNull { it != "null" }
        val format = schema.format

        if (schema.enum?.isNotEmpty() == true) {
            when (type) {
                "string" -> {
                    return EnumGene(name, (schema.enum.map {
                        if (it !is String) {
                            LoggingUtil.uniqueWarn(
                                log,
                                "an item of enum is not string (ie, ${it::class.java.simpleName}) for a property whose `type` is string and `name` is $name"
                            )
                        }
                        it.toString()
                    } as MutableList<String>).apply {
                        if (options.invalidData) {
                            add("EVOMASTER")
                        }
                    }).apply { this.description = schema.description }
                }
                "integer" -> {
                    if (format == "int64") {
                        val data: MutableList<Long> = schema.enum
                            .map { if (it is String) it.toLong() else it as Long }
                            .toMutableList()
                        return EnumGene(name, data.apply { add(42L) })
                    }
                    val data: MutableList<Int> = schema.enum
                        .map { if (it is String) it.toInt() else it as Int }
                        .toMutableList()
                    return EnumGene(name, data.apply { add(42) })
                }
                "number" -> {
                    val data: MutableList<Double> = schema.enum
                        .map { if (it is String) it.toDouble() else it as Double }
                        .toMutableList()
                    return EnumGene(name, data.apply { add(42.0) })
                }
                else -> messages.add("Cannot handle enum of type: $type")
            }
        }

        when (format?.lowercase()) {
            "char" -> return buildStringGeneForChar(name, isInPath)
            "int8", "int16", "int32" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, IntegerGene::class.java, null, isInPath, examples, format, messages
            )
            "int64" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, LongGene::class.java, null, isInPath, examples, messages = messages
            )
            "double" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, DoubleGene::class.java, null, isInPath, examples, messages = messages
            )
            "float" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, FloatGene::class.java, null, isInPath, examples, messages = messages
            )
            "password" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, StringGene::class.java, null, isInPath, examples, messages = messages
            )
            "binary" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, StringGene::class.java, null, isInPath, examples, messages = messages
            )
            "byte" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, Base64StringGene::class.java, null, isInPath, examples, messages = messages
            )
            "date", "local-date" -> return DateGene(name, onlyValidDates = !options.invalidData)
            "date-time", "local-date-time" -> {
                val f = if (format?.lowercase() == "date-time") FormatForDatesAndTimes.RFC3339
                else FormatForDatesAndTimes.ISO_LOCAL
                return DateTimeGene(
                    name,
                    format = f,
                    date = DateGene("date", onlyValidDates = !options.invalidData, format = f),
                    time = TimeGene("time", onlyValidTimes = !options.invalidData, format = f)
                )
            }
            else -> if (format != null) {
                messages.add("Unhandled format '$format' for '$name'")
            }
        }

        when (type?.lowercase()) {
            "integer" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, IntegerGene::class.java, null, isInPath, examples, messages = messages
            )
            "number" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, DoubleGene::class.java, null, isInPath, examples, messages = messages
            )
            "boolean" -> return BooleanGene(name)
            "string" -> {
                return if (schema.pattern == null) {
                    createNonObjectGeneWithSchemaConstraints(
                        schema, name, StringGene::class.java, null, isInPath, examples, messages = messages
                    )
                } else {
                    try {
                        createNonObjectGeneWithSchemaConstraints(
                            schema, name, RegexGene::class.java, null, isInPath, examples, messages = messages
                        )
                    } catch (e: Exception) {
                        LoggingUtil.uniqueWarn(log, "Cannot handle regex: ${schema.pattern}")
                        createNonObjectGeneWithSchemaConstraints(
                            schema, name, StringGene::class.java, null, isInPath, examples, messages = messages
                        )
                    }
                }
            }
            "array" -> {
                if (schema is ArraySchema || schema is JsonSchema) {
                    val arrayType: Schema<*> = if (schema.items == null) {
                        LoggingUtil.uniqueWarn(
                            log,
                            "Array type '$name' is missing mandatory field 'items' to define its type. Defaulting to 'string'"
                        )
                        Schema<Any>().also { it.type = "string" }
                    } else {
                        schema.items
                    }
                    val itemName = schema.xml?.name ?: (name + "_item")
                    val template = getGene(
                        itemName,
                        arrayType,
                        history,
                        referenceClassDef = null,
                        messages = messages
                    )
                    return createNonObjectGeneWithSchemaConstraints(
                        schema, name, ArrayGene::class.java, template, isInPath, examples, messages = messages
                    )
                } else {
                    LoggingUtil.uniqueWarn(log, "Invalid 'array' definition for '$name'")
                }
            }
            "object" -> {
                val properties = schema.properties ?: emptyMap()
                val attributeNames = properties.filterValues { it.xml?.attribute == true }.keys

                if (attributeNames.isNotEmpty()) {
                    val fields = properties.map { (propName, propSchema) ->
                        getGene(propName, propSchema, history, referenceClassDef, false, examples, messages)
                    }
                    return ObjectWithAttributesGene(
                        name = schema.xml?.name ?: name,
                        fixedFields = fields,
                        refType = referenceClassDef,
                        isFixed = true,
                        template = null,
                        additionalFields = null,
                        attributeNames = attributeNames
                    )
                } else {
                    return createObjectGene(name, schema, history, referenceClassDef, examples, messages)
                }
            }
            "file" -> return createNonObjectGeneWithSchemaConstraints(
                schema, name, StringGene::class.java, null, isInPath, examples, messages = messages
            )
        }

        if ((name == "body" || referenceClassDef != null) && schema.properties?.isNotEmpty() == true) {
            return createObjectGene(name, schema, history, referenceClassDef, examples, messages)
        }

        if (type == null && format == null) {
            return createGeneWithUnderSpecificTypeAndSchemaConstraints(
                schema, name, history, referenceClassDef, null, isInPath, examples, messages
            )
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

    private fun createObjectGene(
        name: String,
        schema: Schema<*>,
        history: Deque<String>,
        referenceTypeName: String?,
        examples: List<Pair<Any, String?>>,
        messages: MutableList<String>
    ): Gene {

        val fields = schema.properties?.entries?.map {
            possiblyOptional(
                getGene(it.key, it.value, history, referenceClassDef = null, messages = messages),
                schema.required?.contains(it.key)
            )
        } ?: listOf()

        var additionalFieldTemplate: PairGene<StringGene, Gene>? = null
        var valueTemplate: Gene? = null

        val additional = schema.additionalProperties
        if (additional == null || (additional is Boolean && additional)) {
            // default — see comments in RestActionBuilderV3 history for the
            // taint-map TODO
        }

        if (additional is Schema<*>) {
            if (!additional.`$ref`.isNullOrBlank()) {
                valueTemplate = createObjectFromReference(
                    "valueTemplate",
                    additional.`$ref`,
                    history,
                    examples = examples,
                    messages = messages
                )
            } else if (!additional.type.isNullOrBlank() || additional.types?.isNotEmpty() == true) {
                valueTemplate = getGene(
                    "valueTemplate",
                    additional,
                    history,
                    null,
                    messages = messages
                )
            }
        }

        if (valueTemplate != null) {
            additionalFieldTemplate = PairGene(
                "template",
                StringGene("keyTemplate"),
                valueTemplate.copy()
            )
        }

        val attributeNames = schema.properties
            ?.filter { (_, propSchema) -> propSchema.xml?.attribute == true }
            ?.map { it.key }
            ?: emptyList()

        if (attributeNames.isNotEmpty()) {
            return ObjectWithAttributesGene(
                name = name,
                fixedFields = fields,
                refType = if (schema is ObjectSchema) referenceTypeName ?: schema.title else null,
                isFixed = true,
                template = null,
                additionalFields = null,
                attributeNames = attributeNames.toSet()
            )
        }

        return assembleObjectGeneWithConstraints(
            name, schema, fields, additionalFieldTemplate, history, referenceTypeName, examples, messages
        )
    }

    private fun assembleObjectGeneWithConstraints(
        name: String,
        schema: Schema<*>,
        fields: List<Gene>,
        additionalFieldTemplate: PairGene<StringGene, Gene>?,
        history: Deque<String>,
        referenceTypeName: String?,
        examples: List<Pair<Any, String?>>,
        messages: MutableList<String>
    ): Gene {

        if (!options.enableConstraintHandling)
            return assembleObjectGene(name, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)

        val allOf = schema.allOf?.map { s ->
            getGene(name, s, history, null, messages = messages, examples = examples)
        }
        val anyOf = schema.anyOf?.map { s ->
            getGene(name, s, history, null, messages = messages, examples = examples)
        }

        if (!allOf.isNullOrEmpty() && !anyOf.isNullOrEmpty()) {
            messages.add("Cannot handle allOf and oneOf at same time for a schema with name $name")
            return assembleObjectGene(name, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        val oneOf = schema.oneOf?.map { s ->
            getGene(name, s, history, null, messages = messages)
        }

        if (!oneOf.isNullOrEmpty() && (!allOf.isNullOrEmpty() || !anyOf.isNullOrEmpty())) {
            messages.add("cannot handle oneOf and allOf/oneOf at same time for a schema with name $name")
            return assembleObjectGene(name, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        if (!allOf.isNullOrEmpty()) {
            val allFields = allOf.mapNotNull {
                when (it) {
                    is ObjectGene -> it.fields
                    else -> null
                }
            }.flatten()
            val merged = allFields.plus(fields).distinctBy { it.name }
            return assembleObjectGene(name, schema, merged, additionalFieldTemplate, referenceTypeName, examples, messages)
        }

        if (!oneOf.isNullOrEmpty()) {
            val choices = if (fields.isEmpty()) oneOf
            else listOf(assembleObjectGene(name, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)).plus(oneOf)
            return ChoiceGene(name, choices)
        }

        if (!anyOf.isNullOrEmpty()) {
            val allFields = anyOf.mapNotNull {
                when (it) {
                    is ObjectGene -> it.fields
                    else -> null
                }
            }.flatten()
            val choices = if (anyOf.size > 1) {
                anyOf.plus(
                    assembleObjectGene(
                        name, schema, allFields.plus(fields).distinctBy { it.name },
                        additionalFieldTemplate, referenceTypeName, examples, messages
                    )
                )
            } else anyOf
            return ChoiceGene(name, choices)
        }

        return assembleObjectGene(name, schema, fields, additionalFieldTemplate, referenceTypeName, examples, messages)
    }

    private fun assembleObjectGene(
        name: String,
        schema: Schema<*>,
        fields: List<Gene>,
        additionalFieldTemplate: PairGene<StringGene, Gene>?,
        referenceTypeName: String?,
        otherExampleValues: List<Pair<Any, String?>>,
        messages: MutableList<String>
    ): Gene {

        if (fields.isEmpty()) {
            if (options.usingWhiteBox &&
                schema.additionalProperties == null
                || (schema.additionalProperties is Boolean && schema.additionalProperties == true)
            ) {
                return TaintedMapGene(name, TaintInputName.getTaintName(StaticCounter.getAndIncrease()))
            }
            if (additionalFieldTemplate != null) {
                return FixedMapGene(name, additionalFieldTemplate)
            }
            messages.add("No fields for object definition: $name")
            return FixedMapGene(name, PairGene.createStringPairGene(StringGene(name + "_field"), isFixedFirst = true))
        }

        val mainGene = if (additionalFieldTemplate != null) {
            ObjectGene(name, fields, if (schema is ObjectSchema) referenceTypeName ?: schema.title else null, false, additionalFieldTemplate, mutableListOf())
        } else {
            ObjectGene(name, fields, if (schema is ObjectSchema) referenceTypeName ?: schema.title else null)
        }

        val defaultValue = if (options.probUseDefault > 0) schema.default else null
        val exampleValue = if (options.probUseExamples > 0) schema.example else null
        val multiExampleValues = if (options.probUseExamples > 0) schema.examples else null

        val examples = mutableListOf<Pair<ObjectGene, String?>>()
        if (exampleValue != null) {
            duplicateObjectWithExampleFields(name, mainGene, exampleValue)?.let {
                examples.add(Pair(it, null))
            }
        }
        if (multiExampleValues != null) {
            examples.addAll(multiExampleValues
                .mapNotNull { duplicateObjectWithExampleFields(name, mainGene, it) }
                .map { Pair(it, null) }
            )
        }
        examples.addAll(otherExampleValues
            .mapNotNull {
                duplicateObjectWithExampleFields(name, mainGene, it.first)
                    ?.let { obj -> Pair(obj, it.second) }
            }
        )

        val v = examples.map { it.first }
        val n = examples.map { it.second }

        val exampleGene = if (examples.isNotEmpty()) ChoiceGene(EXAMPLES_NAME, v, valueNames = n) else null
        val defaultGene = if (defaultValue != null) {
            duplicateObjectWithExampleFields("default", mainGene, defaultValue)
        } else null

        return createGeneWithExampleAndDefault(exampleGene, defaultGene, mainGene, name)
    }

    private fun duplicateObjectWithExampleFields(name: String, mainGene: ObjectGene, exampleValue: Any): ObjectGene? {

        if (exampleValue !is ObjectNode) {
            LoggingUtil.uniqueWarn(log, "When building object example, required an ObjectNode, but found a ${exampleValue.javaClass}")
            return null
        }

        val modified = mainGene.fields.map { f ->
            if (exampleValue.has(f.name)) {
                val e = exampleValue.get(f.name)
                if (e.isTextual) {
                    EnumGene<String>(f.name, listOf(asRawString(e.textValue())), 0, false)
                } else if (e.isObject) {
                    val nested = f.getWrappedGene(ObjectGene::class.java)
                    if (nested == null) {
                        LoggingUtil.uniqueWarn(log, "When building object example, cannot handle nested object due to gene type: ${f.javaClass}")
                        f.copy()
                    } else {
                        duplicateObjectWithExampleFields(f.name, nested, e) ?: f.copy()
                    }
                } else {
                    EnumGene<String>(f.name, listOf("" + e.toString()), 0, true)
                }
            } else {
                f.copy()
            }
        }
        return ObjectGene(
            name,
            modified,
            mainGene.refType,
            mainGene.isFixed,
            mainGene.template?.copy() as PairGene<StringGene, Gene>?,
            mainGene.additionalFields?.map { it.copy() as PairGene<StringGene, Gene> }?.toMutableList()
        )
    }

    private fun createGeneWithUnderSpecificTypeAndSchemaConstraints(
        schema: Schema<*>,
        name: String,
        history: Deque<String>,
        referenceTypeName: String?,
        collectionTemplate: Gene?,
        isInPath: Boolean,
        examples: List<Pair<Any, String?>>,
        messages: MutableList<String>
    ): Gene {

        val mightObject = schema.properties?.isNotEmpty() == true
            || referenceTypeName != null
            || containsAllAnyOneOfConstraints(schema)
        if (mightObject) {
            try {
                return createObjectGene(name, schema, history, referenceTypeName, examples, messages)
            } catch (e: Exception) {
                LoggingUtil.uniqueWarn(log, "fail to create ObjectGene for a schema whose `type` and `format` are under specified with error msg: ${e.message ?: "no msg"}")
            }
        }

        LoggingUtil.uniqueWarn(log, "No type/format information provided for '$name'. Defaulting to 'string'")
        return createNonObjectGeneWithSchemaConstraints(
            schema, name, StringGene::class.java, collectionTemplate, isInPath, listOf(), messages = messages
        )
    }

    private fun containsAllAnyOneOfConstraints(schema: Schema<*>) =
        schema.oneOf != null || schema.anyOf != null || schema.allOf != null

    private fun createNonObjectGeneWithSchemaConstraints(
        schema: Schema<*>,
        name: String,
        geneClass: Class<*>,
        collectionTemplate: Gene? = null,
        isInPath: Boolean,
        exampleObjects: List<Pair<Any, String?>>,
        format: String? = null,
        messages: MutableList<String>
    ): Gene {

        val mainGene = createMainGene(schema, geneClass, format, name, isInPath, collectionTemplate)

        val defaultValue = if (options.probUseDefault > 0) schema.default else null
        val exampleValue = if (options.probUseExamples > 0) schema.example else null
        val multiExampleValues = if (options.probUseExamples > 0) schema.examples else null

        val examples = mutableListOf<Pair<String, String?>>()

        if (exampleValue != null) {
            val raw = asRawString(exampleValue)
            examples.add(Pair(raw, null))
            val arrayM = if (raw.startsWith("[")) "If you are wrongly passing to it an array of values, the parser would read it as an array string or simply ignore it. " else ""
            messages.add(
                "The use of 'example' inside a Schema Object is deprecated in OpenAPI. Rather use 'examples'." +
                        " ${arrayM}Read value: $raw"
            )
        }
        if (multiExampleValues != null && multiExampleValues.isNotEmpty()) {
            examples.addAll(multiExampleValues.map { Pair(asRawString(it), null) })
        }
        examples.addAll(exampleObjects.map { Pair(asRawString(it.first), it.second) })

        val defaultGene = if (defaultValue != null) {
            when {
                NumberGene::class.java.isAssignableFrom(geneClass) ->
                    EnumGene("default", listOf(defaultValue.toString()), 0, true)

                geneClass == StringGene::class.java
                    || geneClass == Base64StringGene::class.java
                    || geneClass == RegexGene::class.java ->
                    EnumGene<String>("default", listOf(asRawString(defaultValue)), 0, false)

                else -> {
                    messages.add("Unable to handle 'default': ${asRawString(defaultValue)}")
                    null
                }
            }
        } else null

        val v = examples.map { it.first }
        val n = examples.map { it.second }

        val exampleGene = if (examples.isNotEmpty()) {
            when {
                NumberGene::class.java.isAssignableFrom(geneClass) ->
                    EnumGene(EXAMPLES_NAME, v, 0, true, n)

                geneClass == StringGene::class.java
                    || geneClass == Base64StringGene::class.java
                    || geneClass == RegexGene::class.java ->
                    EnumGene<String>(EXAMPLES_NAME, v, 0, false, n)

                else -> {
                    messages.add("Unable to handle 'examples': ${examples.joinToString(" , ")}")
                    null
                }
            }
        } else null

        return createGeneWithExampleAndDefault(exampleGene, defaultGene, mainGene, name)
    }

    private fun createMainGene(
        schema: Schema<*>,
        geneClass: Class<*>,
        format: String?,
        name: String,
        isInPath: Boolean,
        collectionTemplate: Gene?
    ): Gene {

        val (minInclusive, minimum) = if (!options.enableConstraintHandling) {
            Pair(true, schema.exclusiveMinimumValue ?: schema.minimum)
        } else if (schema.exclusiveMinimumValue != null) {
            Pair(false, schema.exclusiveMinimumValue)
        } else {
            Pair(!(schema.exclusiveMinimum ?: false), schema.minimum)
        }

        val (maxInclusive, maximum) = if (!options.enableConstraintHandling) {
            Pair(true, schema.exclusiveMaximumValue ?: schema.maximum)
        } else if (schema.exclusiveMaximumValue != null) {
            Pair(false, schema.exclusiveMaximumValue)
        } else {
            Pair(!(schema.exclusiveMaximum ?: false), schema.maximum)
        }

        val mainGene = when (geneClass) {
            IntegerGene::class.java -> {
                val minRange: Int
                val maxRange: Int
                if (format == "int8") {
                    minRange = Byte.MIN_VALUE.toInt(); maxRange = Byte.MAX_VALUE.toInt()
                } else if (format == "int16") {
                    minRange = Short.MIN_VALUE.toInt(); maxRange = Short.MAX_VALUE.toInt()
                } else {
                    minRange = Integer.MIN_VALUE; maxRange = Integer.MAX_VALUE
                }

                val minConstraint: Int? = if (options.enableConstraintHandling) minimum?.intValueExact() else null
                val maxConstraint: Int? = if (options.enableConstraintHandling) maximum?.intValueExact() else null

                val minValue = if (minConstraint != null) maxOf(minConstraint, minRange) else minRange
                val maxValue = if (maxConstraint != null) minOf(maxConstraint, maxRange) else maxRange

                IntegerGene(name, min = minValue, max = maxValue, maxInclusive = maxInclusive, minInclusive = minInclusive)
            }
            LongGene::class.java -> LongGene(
                name,
                min = if (options.enableConstraintHandling) minimum?.longValueExact() else null,
                max = if (options.enableConstraintHandling) maximum?.longValueExact() else null,
                maxInclusive = maxInclusive, minInclusive = minInclusive
            )
            FloatGene::class.java -> FloatGene(
                name,
                min = if (options.enableConstraintHandling) minimum?.toFloat() else null,
                max = if (options.enableConstraintHandling) maximum?.toFloat() else null,
                maxInclusive = maxInclusive, minInclusive = minInclusive
            )
            DoubleGene::class.java -> DoubleGene(
                name,
                min = if (options.enableConstraintHandling) minimum?.toDouble() else null,
                max = if (options.enableConstraintHandling) maximum?.toDouble() else null,
                maxInclusive = maxInclusive, minInclusive = minInclusive
            )
            BigDecimalGene::class.java -> BigDecimalGene(
                name,
                min = if (options.enableConstraintHandling) minimum else null,
                max = if (options.enableConstraintHandling) maximum else null,
                maxInclusive = maxInclusive, minInclusive = minInclusive
            )
            BigIntegerGene::class.java -> BigIntegerGene(
                name,
                min = if (options.enableConstraintHandling) minimum?.toBigIntegerExact() else null,
                max = if (options.enableConstraintHandling) maximum?.toBigIntegerExact() else null,
                maxInclusive = maxInclusive, minInclusive = minInclusive
            )
            StringGene::class.java -> buildStringGene(name, schema, isInPath)
            Base64StringGene::class.java -> Base64StringGene(name, buildStringGene(name, schema, isInPath))
            RegexGene::class.java -> RegexHandler.createGeneForEcma262(schema.pattern).apply { this.name = name }
            ArrayGene::class.java -> {
                if (collectionTemplate == null) {
                    throw IllegalArgumentException("cannot create ArrayGene when collectionTemplate is null")
                }
                ArrayGene(
                    name,
                    template = collectionTemplate,
                    uniqueElements = if (options.enableConstraintHandling) schema.uniqueItems ?: false else false,
                    minSize = if (options.enableConstraintHandling) schema.minItems else null,
                    maxSize = if (options.enableConstraintHandling) schema.maxItems else null
                )
            }
            else -> throw IllegalStateException("cannot create gene with constraints for gene:${geneClass.name}")
        }

        if (mainGene.description.isNullOrBlank()) {
            mainGene.description = schema.description
        }
        return mainGene
    }

    private fun createGeneWithExampleAndDefault(
        exampleGene: Gene?,
        defaultGene: Gene?,
        mainGene: Gene,
        name: String
    ): Gene {
        if (exampleGene == null && defaultGene == null) return mainGene

        if (exampleGene != null && defaultGene != null) {
            val pd = options.probUseDefault
            val pe = options.probUseExamples
            val pm = 1 - pd - pe
            return ChoiceGene(name, listOf(defaultGene, exampleGene, mainGene), 0, listOf(pd, pe, pm))
        }
        if (exampleGene != null) {
            val pe = options.probUseExamples
            val pm = 1 - pe
            return ChoiceGene(name, listOf(exampleGene, mainGene), 0, listOf(pe, pm))
        }
        if (defaultGene != null) {
            val pd = options.probUseDefault
            val pm = 1 - pd
            return ChoiceGene(name, listOf(defaultGene, mainGene), 0, listOf(pd, pm))
        }
        throw IllegalStateException("BUG: logic error, this code should never be reached")
    }

    private fun asRawString(x: Any): String {
        val s = x.toString()
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length - 1)
        return s
    }

    private fun buildStringGeneForChar(name: String, isInPath: Boolean): StringGene {
        return StringGene(
            name,
            minLength = 1,
            maxLength = 1,
            invalidChars = if (isInPath) listOf('/', '.') else listOf()
        )
    }

    private fun buildStringGene(name: String, schema: Schema<*>, isInPath: Boolean): StringGene {
        val defaultMin = if (isInPath) 1 else 0
        return StringGene(
            name,
            maxLength = if (options.enableConstraintHandling) schema.maxLength
                ?: EMConfig.stringLengthHardLimit else EMConfig.stringLengthHardLimit,
            minLength = max(defaultMin, if (options.enableConstraintHandling) schema.minLength ?: 0 else 0),
            invalidChars = if (isInPath) listOf('/', '.') else listOf()
        ).apply { this.description = schema.description }
    }

    private fun getClassDef(reference: String) = reference.substring(reference.lastIndexOf("/") + 1)
}
