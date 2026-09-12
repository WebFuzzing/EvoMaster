package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.webfuzzing.asyncapi.models.AsyncApiMessage
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver

/**
 * Recognises which of the messages a contract declares for a reply an observed reply is.
 *
 * A reply channel often carries several messages -- a result and an error, say -- and telling
 * them apart is what gives a black-box search distinct outcomes to cover. There are no status
 * codes to read, so the payload is matched against each declared schema instead.
 *
 * The matching is structural and deliberately lenient: it checks the parts of JSON Schema that
 * tell one message from another (type, required fields, const and enum discriminators, the
 * combinators), and gives the benefit of the doubt on anything it does not understand. It is a
 * classifier, not a validator: its job is to tell which declared message a reply is, not to
 * find every way in which it deviates from its schema.
 */
object AsyncApiReplyClassifier {

    private const val REF = "\$ref"
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
     * How far to follow references and nesting before giving up. A schema that refers to
     * itself is legitimate, and the payload it describes is finite, so this is only reached by
     * a cycle in the schema that the data never enters.
     */
    private const val MAX_DEPTH = 32

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
        } catch (e: JsonProcessingException) {
            //not JSON, so it is none of the JSON-described messages
            return null
        } ?: return null

        return candidates
            .filter { it.payload != null && matches(node, it.payload, componentSchemas, 0) }
            .maxByOrNull { specificity(it.payload, componentSchemas) }
    }

    private fun matches(node: JsonNode, schema: JsonNode, schemas: Map<String, JsonNode>, depth: Int): Boolean {

        if (depth > MAX_DEPTH) {
            return true
        }

        //a reference that cannot be followed is something this cannot judge, so it does not reject
        val s = resolve(schema, schemas) ?: return true

        if (!s.isObject) {
            return true
        }

        s.get(CONST)?.let { if (node != it) return false }

        s.get(ENUM)?.let { allowed -> if (allowed.isArray && allowed.none { it == node }) return false }

        s.get(TYPE)?.let { if (!isOfType(node, it)) return false }

        s.get(ALL_OF)?.let { all -> if (all.any { !matches(node, it, schemas, depth + 1) }) return false }

        s.get(ANY_OF)?.let { any -> if (any.none { matches(node, it, schemas, depth + 1) }) return false }

        //oneOf is read as "at least one": exclusivity is a validator's concern, not a classifier's
        s.get(ONE_OF)?.let { one -> if (one.none { matches(node, it, schemas, depth + 1) }) return false }

        if (node.isObject) {
            s.get(REQUIRED)?.let { required -> if (required.any { !node.has(it.asText()) }) return false }

            s.get(PROPERTIES)?.fields()?.forEach { (name, property) ->
                val value = node.get(name)
                if (value != null && !matches(value, property, schemas, depth + 1)) {
                    return false
                }
            }
        }

        if (node.isArray) {
            s.get(ITEMS)?.let { items ->
                if (items.isObject && node.any { !matches(it, items, schemas, depth + 1) }) return false
            }
        }

        return true
    }

    /**
     * Whether [node] is of one of the types [type] names. JSON Schema writes it as one name or a
     * list of them, and counts a number with no fractional part as an integer.
     */
    private fun isOfType(node: JsonNode, type: JsonNode): Boolean {

        val names = if (type.isArray) type.map { it.asText() } else listOf(type.asText())

        return names.any { name ->
            when (name) {
                TYPE_OBJECT -> node.isObject
                TYPE_ARRAY -> node.isArray
                TYPE_STRING -> node.isTextual
                TYPE_INTEGER -> node.isIntegralNumber
                        || (node.isNumber && node.decimalValue().stripTrailingZeros().scale() <= 0)
                TYPE_NUMBER -> node.isNumber
                TYPE_BOOLEAN -> node.isBoolean
                TYPE_NULL -> node.isNull
                else -> true
            }
        }
    }

    /**
     * The schema itself, once any chain of `$ref` to a component schema is followed. Null when
     * a reference points at something other than a whole component schema.
     */
    private fun resolve(schema: JsonNode, schemas: Map<String, JsonNode>): JsonNode? {

        var current = schema

        repeat(MAX_DEPTH) {
            val ref = AsyncApiRefResolver.refOf(current) ?: return current
            val key = AsyncApiRefResolver.refKey(ref, AsyncApiRefResolver.SCHEMA_PREFIX) ?: return null
            current = schemas[key] ?: return null
        }

        return null
    }

    /**
     * How many fields the schema pins down at its top level, which is what tells a specific
     * message from a permissive one when both match.
     */
    private fun specificity(schema: JsonNode, schemas: Map<String, JsonNode>): Int {

        val s = resolve(schema, schemas) ?: return 0

        val required = s.get(REQUIRED)?.size() ?: 0
        val pinned = s.get(PROPERTIES)?.count { it.has(CONST) || it.has(ENUM) } ?: 0

        return required + pinned
    }
}
