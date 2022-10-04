package org.evomaster.core.problem.external.service.httpws

import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.MapGene
import org.evomaster.core.search.gene.collection.PairGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object HttpExternalServiceResponseBuilder {

    private val log: Logger = LoggerFactory.getLogger(RestActionBuilderV3::class.java)

    fun buildResponseGene(name: String, schema: String) : Gene {
        val schema = """
            {
                "openapi": "3.0.0",
                "components": {
                    "schemas": {
                        $schema
                    }
                }
            }          
        """.trimIndent()

        val swagger = OpenAPIParser().readContents(schema,null,null).openAPI
        val gene = createObjectGene(
            name,
            swagger.components.schemas[name]!!,
            swagger,
            ArrayDeque(),
            name
        )
        return gene.copy()
    }

    private fun createObjectGene(name: String, schema: Schema<*>, swagger: OpenAPI, history: Deque<String>, referenceTypeName: String?) : Gene {
        val fields = schema.properties?.entries?.map {
            possiblyOptional(
                getGene(it.key, it.value, swagger, history, referenceClassDef = null),
                schema.required?.contains(it.key)
            )
        } ?: listOf()

        /*
             Can be either a boolean or a Schema
          */
        val additional = schema.additionalProperties

        if (additional is Boolean) {
            /*
                if 'false', no other fields besides the specified ones can be added.
                Default is 'true'.
              */
            //TODO could add extra fields for robustness testing
        }
        if (additional is Schema<*>) {
            /*
               TODO could add extra fields for robustness testing,
               with and without following the given schema for their type
             */

            /*
                TODO proper handling.
                Using a map is just a temp solution
             */

            if (fields.isEmpty()) {
                // here, the first of pairgene should not be mutable
                return MapGene(name, PairGene.createStringPairGene(getGene(name + "_field", additional, swagger, history, null), isFixedFirst = true))
            }
        }

        return ObjectGene(name, fields, if(schema is ObjectSchema) referenceTypeName?:schema.title else null)
    }

    private fun possiblyOptional(gene: Gene, required: Boolean?): Gene {

        if (required != true) {
            return OptionalGene(gene.name, gene).also { GeneUtils.preventCycles(it) }
        }

        return gene
    }

    private fun getGene(
        name: String,
        schema: Schema<*>,
        swagger: OpenAPI,
        history: Deque<String> = ArrayDeque<String>(),
        referenceClassDef: String?
    ): Gene {

//        if (!schema.`$ref`.isNullOrBlank()) {
//            return createObjectFromReference(name, schema.`$ref`, swagger, history)
//        }


        /*
            https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#dataTypeFormat

        Common Name	    type	format	Comments
        integer	        integer	int32	signed 32 bits
        long	        integer	int64	signed 64 bits
        float	        number	float
        double	        number	double
        string	        string
        byte	        string	byte	base64 encoded characters
        binary	        string	binary	any sequence of octets
        boolean	        boolean
        date	        string	date	As defined by full-date - RFC3339
        dateTime	    string	date-time	As defined by date-time - RFC3339
        password	    string	password	Used to hint UIs the input needs to be obscured.
         */

        val type = schema.type
        val format = schema.format

        if (schema.enum?.isNotEmpty() == true) {

            //Besides the defined values, add one to test robustness
            when (type) {
                "string" ->
                    return EnumGene(name, (schema.enum as MutableList<String>).apply { add("EVOMASTER") })
                /*
                    Looks like a possible bug in the parser, where numeric enums can be read as strings... got this
                    issue in GitLab schemas, eg for visibility_level
                 */
                "integer" -> {
                    if (format == "int64") {
                        val data : MutableList<Long> = schema.enum
                            .map{ if(it is String) it.toLong() else it as Long}
                            .toMutableList()

                        return EnumGene(name, (data).apply { add(42L) })
                    }

                    val data : MutableList<Int> = schema.enum
                        .map{ if(it is String) it.toInt() else it as Int}
                        .toMutableList()
                    return EnumGene(name, data.apply { add(42) })
                }
                "number" -> {
                    //if (format == "double" || format == "float") {
                    //TODO: Is it always casted as Double even for Float??? Need test
                    val data : MutableList<Double> = schema.enum
                        .map{ if(it is String) it.toDouble() else it as Double}
                        .toMutableList()
                    return EnumGene(name, data.apply { add(42.0) })
                }
                else -> log.warn("Cannot handle enum of type: $type")
            }
        }

        /*
            TODO constraints like min/max
         */

        //first check for "optional" format
        when (format?.lowercase()) {
            "int32" -> return IntegerGene(name)
            "int64" -> return LongGene(name)
            "double" -> return DoubleGene(name)
            "float" -> return FloatGene(name)
            "password" -> return StringGene(name) //nothing special to do, it is just a hint
            "binary" -> return StringGene(name) //does it need to be treated specially?
            "byte" -> return Base64StringGene(name)
            "date" -> return DateGene(name)
            "date-time" -> return DateTimeGene(name)
            else -> if (format != null) {
                LoggingUtil.uniqueWarn(log, "Unhandled format '$format'")
            }
        }

        /*
                If a format is not defined, the type should default to
                the JSON Schema definition
         */
        when (type?.lowercase()) {
            "integer" -> return IntegerGene(name)
            "number" -> return DoubleGene(name)
            "boolean" -> return BooleanGene(name)
            "string" -> {
                return if (schema.pattern == null) {
                    StringGene(name)
                } else {
                    try {
                        RegexHandler.createGeneForEcma262(schema.pattern).apply { this.name = name }
                    } catch (e: Exception) {
                        /*
                            TODO: if the Regex is syntactically invalid, we should warn
                            the user. But, as we do not support 100% regex, might be an issue
                            with EvoMaster. Anyway, in such cases, instead of crashing EM, let's just
                            take it as a String.
                            When 100% support, then tell user that it is his/her fault
                         */
                        LoggingUtil.uniqueWarn(log, "Cannot handle regex: ${schema.pattern}")
                        StringGene(name)
                    }
                }
            }
            "array" -> {
                if (schema is ArraySchema) {

                    val arrayType: Schema<*> = if (schema.items == null) {
                        LoggingUtil.uniqueWarn(
                            log, "Array type '$name' is missing mandatory field 'items' to define its type." +
                                " Defaulting to 'string'")
                        Schema<Any>().also { it.type = "string" }
                    } else {
                        schema.items
                    }
                    val template = getGene(name + "_item", arrayType, swagger, history, referenceClassDef = null)

                    //Could still have an empty []
//                    if (template is CycleObjectGene) {
//                        return CycleObjectGene("<array> ${template.name}")
//                    }
                    return ArrayGene(name, template)
                } else {
                    LoggingUtil.uniqueWarn(log, "Invalid 'array' definition for '$name'")
                }
            }

            "object" -> {
                return createObjectGene(name, schema, swagger, history, referenceClassDef)
            }

            "file" -> return StringGene(name) //TODO file is a hack. I want to find a more elegant way of dealing with it (BMR)
        }

        if (name == "body" && schema.properties?.isNotEmpty() == true) {
            /*
                This could happen when parsing a body-payload as formData
            */
            return createObjectGene(name, schema, swagger, history, referenceClassDef)
        }

        if (type == null && format == null) {
            LoggingUtil.uniqueWarn(log, "No type/format information provided for '$name'. Defaulting to 'string'")
            return StringGene(name)
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }
}