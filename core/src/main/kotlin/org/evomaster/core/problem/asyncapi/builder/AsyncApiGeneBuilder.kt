package org.evomaster.core.problem.asyncapi.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import com.webfuzzing.asyncapi.models.AsyncApiMessage
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.gene.Gene

/**
 * Turns the JSON Schema of an AsyncAPI message into the genes the search mutates.
 *
 * The parser deliberately stops at the schema: it leaves every `$ref` inside a payload alone,
 * and guarantees that whatever those references reach is present in
 * [AsyncApiDocument.getComponentSchemas]. That guarantee is what this builder trades on -- it hands
 * the whole schema map to [RestActionBuilderV3.createGeneForDTO], which wraps it in a synthetic
 * OpenAPI document and lets the existing machinery resolve the references and build the genes.
 *
 * Reusing that machinery rather than writing a second JSON-Schema-to-gene converter means
 * AsyncAPI payloads get everything REST already has: numeric and length bounds, formats,
 * `oneOf` as a choice, cycle detection, and optional-versus-required fields.
 */
object AsyncApiGeneBuilder {

    /**
     * Under what name a payload written inline, rather than as a reference to a named schema,
     * is added to the schema map. The prefix keeps it from colliding with a declared schema.
     */
    private const val INLINE_PREFIX = "_asyncapi_"

    /**
     * The genes for a message's payload, or null when it declares none.
     */
    fun buildPayloadGene(
        schema: AsyncApiDocument,
        message: AsyncApiMessage,
        options: RestActionBuilderV3.Options
    ): Gene? = build(message.payload, "${message.id}.payload", schema, options)

    /**
     * The genes for a message's headers, or null when it declares none.
     *
     * Headers are built separately from the payload because they travel separately on the wire:
     * a transport with metadata puts them beside the body rather than in it.
     */
    fun buildHeadersGene(
        schema: AsyncApiDocument,
        message: AsyncApiMessage,
        options: RestActionBuilderV3.Options
    ): Gene? = build(message.headers, "${message.id}.headers", schema, options)

    /**
     * Options for building AsyncAPI payloads.
     *
     * Note `invalidData = false`, which is not what REST does. That flag makes the builder add
     * a bogus "EVOMASTER" member to every enum, on purpose, to probe how a service handles a
     * value it never declared. In a message payload that backfires: an enum of one value is how
     * documents write a routing discriminator (`request: {const: list_legs}`, which arrives
     * here as an enum), and a message carrying a discriminator the service does not recognise
     * is silently dropped. Half the messages sent would then go nowhere -- wasting executions,
     * and firing the no-reply fault target for a fault of our own making.
     *
     * Nothing is lost for reaching error paths: the bounds that matter are preserved, so a
     * field declared `minimum: 3` is still fuzzed across its boundary.
     */
    fun options(config: EMConfig) = RestActionBuilderV3.Options(
        enableConstraintHandling = config.enableSchemaConstraintHandling,
        invalidData = false,
        usingWhiteBox = !config.blackBox,
        enableAdvancedFormats = config.enableAdvancedFormats,
        inferFormatFromNames = config.inferFormatFromNames
    )

    private fun build(
        declared: JsonNode?,
        inlineName: String,
        schema: AsyncApiDocument,
        options: RestActionBuilderV3.Options
    ): Gene? {

        if (declared == null) {
            return null
        }

        //a reference that is the whole declaration, so that following it loses nothing
        val ref = AsyncApiRefResolver.refOf(declared)?.takeIf { isWholeSchemaRef(declared) }

        /*
            A payload that is just a reference to a declared schema is built under that schema's
            own name, which keeps the gene named after what the document calls it. Anything else
            is added to the map under a name of our own.

            Note `refKey` rather than `schemaKeyOf`: only a pointer at the schema itself may be
            shortcut this way. One that goes deeper names a part of it, and building the whole
            schema instead would be a different message altogether.
         */
        val referenced = ref
            ?.let { AsyncApiRefResolver.refKey(it, AsyncApiRefResolver.SCHEMA_PREFIX) }
            ?.takeIf { schema.componentSchemas.containsKey(it) }

        val name = referenced ?: (INLINE_PREFIX + inlineName)

        val schemas = JsonNodeFactory.instance.objectNode()
        schema.componentSchemas.forEach { (key, node) -> schemas.set<JsonNode>(key, usable(node)) }
        if (referenced == null) {
            schemas.set<JsonNode>(name, usable(pointedAt(ref, declared, schema)))
        }

        //the format createGeneForDTO expects: the name of the wanted schema, then all of them
        return RestActionBuilderV3.createGeneForDTO(name, "\"$name\":$schemas", options)
    }

    /**
     * What a payload stands for when its reference goes deeper than the schema it names.
     *
     * `#/components/schemas/Order/properties/item` is a legitimate way of saying "the shape of
     * that one property", and the parser lets it through: what it checks is that `Order` is
     * present. It cannot be left to be resolved further down, though. The OpenAPI machinery the
     * genes are built with follows a reference to a whole schema and nothing else, and answers
     * a deeper one with an empty object -- which would build a message with no fields at all,
     * and say nothing about it.
     *
     * So the pointer is walked here, against the very component schemas the parser guaranteed
     * are reachable. Anything else is returned untouched.
     */
    private fun pointedAt(ref: String?, declared: JsonNode, schema: AsyncApiDocument): JsonNode {

        if (ref == null) {
            return declared
        }

        val tail = ref.removePrefix(AsyncApiRefResolver.SCHEMA_PREFIX).substringAfter('/', "")

        if (tail.isEmpty()) {
            //the reference names a whole schema, which needs no walking
            return declared
        }

        val target = AsyncApiRefResolver.schemaKeyOf(ref)?.let { schema.componentSchemas[it] }
            ?: return declared

        var current = target

        for (segment in tail.split("/")) {
            current = current.get(decodePointerSegment(segment))
                ?: throw IllegalArgumentException(
                    "The schema refers to '$ref', but '$segment' is not there, so there is no" +
                            " shape to build from"
                )
        }

        if (!current.isObject) {
            throw IllegalArgumentException(
                "The schema refers to '$ref', which is not a JSON Schema object, so there is no" +
                        " shape to build from"
            )
        }

        return current
    }

    /**
     * JSON Pointer escaping: "~1" is a "/" and "~0" is a "~", undone in that order.
     */
    private fun decodePointerSegment(segment: String) =
        segment.replace("~1", "/").replace("~0", "~")

    /**
     * Whether the node is nothing but a reference, so that following it loses nothing.
     */
    private fun isWholeSchemaRef(node: JsonNode) =
        node.isObject && node.fieldNames().asSequence().toList() == listOf("\$ref")

    /**
     * A copy of the schema with the constructs the gene builder cannot read translated into
     * ones it can. The original is left alone, as it belongs to the parsed document.
     */
    private fun usable(node: JsonNode): JsonNode = rewriteConst(node.deepCopy())

    /**
     * Rewrite every `const` into whatever pins a field to that one value in a form the gene
     * builder reads.
     *
     * `const` is JSON Schema's way of fixing a field to one literal, and documents use it to
     * mark which message a payload is. The gene builder does not read it at all -- with or
     * without a `type` beside it, a `const` field becomes a free value, so the discriminator
     * would be random and the service would not recognise the message.
     *
     * Which rewrite is used depends on the kind of literal, because the two available ways of
     * saying "only this value" do not both work for every type:
     *
     * - **text** becomes a single-valued `enum`. Note the builder adds a bogus member to a
     *   string enum when asked for invalid data, which is why [options] turns that off.
     * - **numbers** become equal bounds instead. An enum would be read, but the bogus member
     *   added to a numeric enum is not conditional on anything, so an enum of one value would
     *   always come back as two. Equal bounds pin the value with no such surprise.
     * - **booleans** are left alone. There is no enum handling for them and no bounds to set;
     *   a two-valued field is guessed half the time anyway.
     *
     * Only `const` in the position of a keyword is rewritten. The same word can appear as a
     * field name a document declares, or inside a `default` that happens to have a member of
     * that name, and rewriting either would corrupt the document rather than help it -- hence
     * [DATA_KEYWORDS] and [SCHEMA_MAPS].
     */
    private fun rewriteConst(node: JsonNode): JsonNode {

        if (node.isArray) {
            node.forEach { rewriteConst(it) }
            return node
        }

        if (!node.isObject) {
            return node
        }

        val obj = node as ObjectNode
        val const = obj.get("const")

        if (const != null && !const.isContainerNode) {
            when {
                const.isTextual -> {
                    obj.remove("const")
                    obj.putArray("enum").add(const)
                    obj.put("type", "string")
                }
                const.isNumber -> {
                    obj.remove("const")
                    obj.set<JsonNode>("minimum", const)
                    obj.set<JsonNode>("maximum", const)
                    obj.put("type", if (const.isIntegralNumber) "integer" else "number")
                }
                const.isBoolean -> {
                    obj.remove("const")
                    obj.put("type", "boolean")
                }
            }
        }

        obj.fields().forEach { (key, value) ->
            when (key) {
                /*
                    These map names a document chose to the schemas describing them, so their
                    keys are never keywords: whatever is under them is a schema and is walked,
                    but this level itself is not read as one.
                 */
                in SCHEMA_MAPS -> value.fields().forEach { (_, member) -> rewriteConst(member) }
                //literal data, where a member named "const" is a value and not a keyword
                in DATA_KEYWORDS -> Unit
                else -> rewriteConst(value)
            }
        }

        return obj
    }

    /**
     * The keywords whose value is literal data rather than a schema, so nothing inside them is
     * a keyword either.
     */
    private val DATA_KEYWORDS = setOf("const", "default", "enum", "example", "examples")

    /**
     * The keywords whose value maps arbitrary names to schemas. Their keys come from the
     * document, so a field a service happens to call "const" or "default" must still be walked.
     */
    private val SCHEMA_MAPS = setOf("properties", "patternProperties", "definitions", "\$defs")
}
