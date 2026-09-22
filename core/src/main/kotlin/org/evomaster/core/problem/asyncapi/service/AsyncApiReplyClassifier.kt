package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.webfuzzing.asyncapi.models.AsyncApiMessage
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver

/**
 * Recognises which of the messages a contract declares for a reply an observed reply is, by
 * matching its payload against each declared schema.
 *
 * It is a classifier rather than a validator: it reads what tells the declared messages apart
 * and gives the benefit of the doubt on anything it cannot read, because a reply it fails to
 * recognise is reported as a fault. Nothing else here has to do this -- REST is told which
 * response applies by the status code, GraphQL by its `errors` field, and RPC by the driver --
 * so there was no existing answer to borrow.
 *
 * The matching is done by hand rather than with a schema validator. The one already on the
 * classpath, pulled in by swagger-request-validator, understands draft-04 only, and so does not
 * know `const`: the very keyword AsyncAPI documents lean on to tell message variants apart, and
 * the reason [org.evomaster.core.problem.asyncapi.builder.AsyncApiGeneBuilder] has to rewrite it
 * before the gene builder sees it. A validator that reads a modern draft would replace most of
 * this, at the cost of a new dependency.
 */
object AsyncApiReplyClassifier {

    private const val REF = "\$ref"
    private const val PATH_SEPARATOR = "/"
    private const val TYPE = "type"
    private const val PROPERTIES = "properties"
    private const val REQUIRED = "required"
    private const val ITEMS = "items"
    private const val CONST = "const"
    private const val ENUM = "enum"
    private const val ONE_OF = "oneOf"
    private const val ANY_OF = "anyOf"
    private const val ALL_OF = "allOf"

    private const val TYPE_OBJECT = "object"
    private const val TYPE_ARRAY = "array"
    private const val TYPE_STRING = "string"
    private const val TYPE_INTEGER = "integer"
    private const val TYPE_NUMBER = "number"
    private const val TYPE_BOOLEAN = "boolean"
    private const val TYPE_NULL = "null"

    /**
     * How deep to descend into a schema before giving up and accepting what is left. Nesting and
     * the combinators both count towards it, so a deeply combinated schema can reach it.
     */
    private const val MAX_SCHEMA_DEPTH = 32

    /**
     * How many `$ref` hops to follow before deciding the references form a cycle.
     */
    private const val MAX_REF_CHAIN = 32

    private val mapper = ObjectMapper()

    /**
     * The declared message [payload] is an instance of, or null when it is none of them.
     *
     * When more than one matches, the most specific wins: the one pinning down the most fields,
     * through `required` and through `const`/`enum` properties. Ties go to the first declared.
     *
     * @param candidates the messages the contract declares for the reply, in declaration order
     * @param componentSchemas where a `$ref` inside a payload schema is resolved
     */
    fun classify(
        payload: String?,
        candidates: List<AsyncApiMessage>,
        componentSchemas: Map<String, JsonNode>
    ): AsyncApiMessage? {

        if (payload == null) {
            return null
        }

        val node = try {
            mapper.readTree(payload)
        } catch (_: JsonProcessingException) {
            //not JSON, so it is none of the JSON-described messages
            return null
        }

        //an empty body parses to nothing at all, which is no message either
        if (node == null || node.isMissingNode) {
            return null
        }

        return candidates
            .filter { it.payload != null && matches(node, it.payload, componentSchemas, 0) }
            .maxByOrNull { specificity(it.payload, componentSchemas) }
    }

    private fun matches(node: JsonNode, schema: JsonNode, schemas: Map<String, JsonNode>, depth: Int): Boolean {

        if (depth >= MAX_SCHEMA_DEPTH) {
            return true
        }

        //a reference that cannot be followed is something this cannot judge, so it does not reject
        val resolved = resolve(schema, schemas) ?: return true

        if (!resolved.isObject) {
            return true
        }

        val const = resolved.get(CONST)
        if (const != null && !sameValue(node, const)) {
            return false
        }

        val allowed = resolved.get(ENUM)
        if (allowed != null && allowed.isArray && allowed.none { sameValue(node, it) }) {
            return false
        }

        val type = resolved.get(TYPE)
        if (type != null && !isOfType(node, type)) {
            return false
        }

        val all = getBranches(resolved, ALL_OF)
        if (all != null && all.any { !matches(node, it, schemas, depth + 1) }) {
            return false
        }

        val any = getBranches(resolved, ANY_OF)
        if (any != null && any.none { matches(node, it, schemas, depth + 1) }) {
            return false
        }

        //oneOf is read as "at least one": exclusivity is a validator's concern, not a classifier's
        val one = getBranches(resolved, ONE_OF)
        if (one != null && one.none { matches(node, it, schemas, depth + 1) }) {
            return false
        }

        if (node.isObject) {
            resolved.get(REQUIRED)?.let { required -> if (required.any { !node.has(it.asText()) }) return false }

            resolved.get(PROPERTIES)?.fields()?.forEach { (name, property) ->
                val value = node.get(name)
                if (value != null && !matches(value, property, schemas, depth + 1)) {
                    return false
                }
            }
        }

        if (node.isArray) {
            resolved.get(ITEMS)?.let { items ->
                if (items.isObject && node.any { !matches(it, items, schemas, depth + 1) }) return false
            }
        }

        return true
    }

    /**
     * The branches of a combinator, or null when it is not a usable list of them. A combinator
     * written as anything but a non-empty array says nothing, and must not reject everything.
     */
    private fun getBranches(schema: JsonNode, keyword: String): List<JsonNode>? {

        val branches = schema.get(keyword) ?: return null

        return if (branches.isArray && !branches.isEmpty) branches.toList() else null
    }

    /**
     * Whether two JSON values are the same as JSON Schema counts sameness. Numbers compare by
     * value, so that a schema written `const: 1.0` accepts a reply carrying `1`; Jackson's own
     * equality would say those differ, being of different node types.
     */
    private fun sameValue(node: JsonNode, other: JsonNode): Boolean {

        if (node.isNumber && other.isNumber && hasDecimalValue(node) && hasDecimalValue(other)) {
            return node.decimalValue().compareTo(other.decimalValue()) == 0
        }

        return node == other
    }

    /**
     * Whether the number has a decimal value at all. Only a floating-point node can hold an
     * infinity or a NaN, and asking those for a [java.math.BigDecimal] throws.
     */
    private fun hasDecimalValue(node: JsonNode): Boolean {
        return !(node.isDouble || node.isFloat) || node.doubleValue().isFinite()
    }

    /**
     * Whether [node] is of one of the types [type] names. JSON Schema writes it as one name or a
     * list of them, and counts a number with no fractional part as an integer.
     *
     * A `type` that is neither is not something this can read, so nothing is rejected on it.
     */
    private fun isOfType(node: JsonNode, type: JsonNode): Boolean {

        val names = when {
            type.isArray && !type.isEmpty -> type.map { it.asText() }
            type.isTextual -> listOf(type.asText())
            else -> return true
        }

        return names.any { name ->
            when (name) {
                TYPE_OBJECT -> node.isObject
                TYPE_ARRAY -> node.isArray
                TYPE_STRING -> node.isTextual
                //a whole number written with a decimal point is an integer; canConvertToExactIntegral
                //also answers for a value too large to be a BigDecimal, where decimalValue() throws
                TYPE_INTEGER -> node.isIntegralNumber || (node.isNumber && node.canConvertToExactIntegral())
                TYPE_NUMBER -> node.isNumber
                TYPE_BOOLEAN -> node.isBoolean
                TYPE_NULL -> node.isNull
                else -> true
            }
        }
    }

    /**
     * The schema itself, once any chain of `$ref` has been followed. Null when a reference leads
     * nowhere, which is the one case this cannot judge.
     *
     * A pointer may go deeper than the schema it names, as in
     * `#/components/schemas/Order/properties/item`, which is a legitimate way of saying "the
     * shape of that one property".
     */
    private fun resolve(schema: JsonNode, schemas: Map<String, JsonNode>, depth: Int = 0): JsonNode? {

        if (depth >= MAX_REF_CHAIN) {
            return null
        }

        var current = schema

        repeat(MAX_REF_CHAIN - depth) {

            val ref = AsyncApiRefResolver.refOf(current) ?: return current
            val key = AsyncApiRefResolver.schemaKeyOf(ref) ?: return null
            val target = schemas[key] ?: return null

            val pointer = ref.removePrefix(AsyncApiRefResolver.SCHEMA_PREFIX).substringAfter(PATH_SEPARATOR, "")

            if (pointer.isEmpty()) {
                current = target
            } else {
                //the schema the pointer goes into may itself be a reference, so follow that first
                val base = resolve(target, schemas, depth + 1) ?: return null
                current = base.at(PATH_SEPARATOR + pointer)
                if (current.isMissingNode) {
                    return null
                }
            }
        }

        return null
    }

    /**
     * How many fields a schema pins down, which is what tells a specific message from a
     * permissive one when a reply matches both.
     *
     * Counted through the combinators as well: a message that says what it requires inside an
     * `allOf` is no less specific for having written it that way.
     */
    private fun specificity(schema: JsonNode, schemas: Map<String, JsonNode>, depth: Int = 0): Int {

        if (depth >= MAX_SCHEMA_DEPTH) {
            return 0
        }

        val resolved = resolve(schema, schemas) ?: return 0

        if (!resolved.isObject) {
            return 0
        }

        val required = resolved.get(REQUIRED)?.size() ?: 0

        val pinned = resolved.get(PROPERTIES)?.count { property ->
            val target = resolve(property, schemas)
            target != null && (target.has(CONST) || target.has(ENUM))
        } ?: 0

        //every branch of an allOf has to hold, so all of them count
        val fromAll = getBranches(resolved, ALL_OF)
            ?.sumOf { specificity(it, schemas, depth + 1) } ?: 0

        //only one branch of a choice has to hold, so it is worth what its weakest branch is
        val fromChoice = listOf(ANY_OF, ONE_OF).sumOf { keyword ->
            getBranches(resolved, keyword)?.minOf { specificity(it, schemas, depth + 1) } ?: 0
        }

        return required + pinned + fromAll + fromChoice
    }
}
