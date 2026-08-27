package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Translates a JSON Schema (used by MCP tool `inputSchema`) into a schema node compatible with the OpenAPI 3.0.x parser used by
 * [org.evomaster.core.problem.rest.builder.RestActionBuilderV3].
 *
 * The result is a flat map of schema-name -> normalized schema node: the root schema is keyed by
 * the given root name, and every `$defs`/`definitions` entry is lifted to a sibling entry so that
 * `$ref`s (rewritten to `#/components/schemas/<name>`) resolve across the set.
 */
object JsonSchemaNormalizer {

    private val mapper = ObjectMapper()

    private const val DEFS = "\$defs"
    private const val LEGACY_DEFS = "definitions"

    /**
     * Normalize an [inputSchema] into a flat, OpenAPI-3.0-compatible
     * map of schemas.
     *
     * @param rootName component name under which the root schema is registered
     * @param inputSchema the raw JSON Schema node (typically an MCP tool `inputSchema`)
     * @param messages list for warnings about lossy or unsupported constructs
     * @return schemas keyed by component name; always contains [rootName]
     */
    fun normalize(rootName: String, inputSchema: JsonNode, messages: MutableList<String>): Map<String, JsonNode> {

        val result = LinkedHashMap<String, JsonNode>()

        // Work on a deep copy so the caller's node is never mutated.
        var rootCopy = inputSchema.deepCopy<JsonNode>()

        val typeNode = rootCopy.get("type")
        val isObject = rootCopy is ObjectNode && (typeNode == null || typeNode.asText() == "object" || (typeNode.isArray && typeNode.any { it.asText() == "object" }))

        if (!isObject) {
            messages.add("Schema at $rootName expected to be an object; replaced with empty object")
            val newRoot = mapper.createObjectNode()
            newRoot.put("type", "object")
            rootCopy = newRoot
        } else if (!rootCopy.has("type")) {
            rootCopy.put("type", "object")
        }

        // Collect def names first so ref rewriting is aware of every sibling.
        val defNames = HashSet<String>()
        collectDefNames(rootCopy, defNames)

        // Lift defs into siblings, disambiguating against the root name.
        val defRename = HashMap<String, String>()
        for (name in defNames) {
            defRename[name] = if (name == rootName) "${name}_def" else name
        }

        // Extract and normalize defs first: normalizeNode strips `$defs` as it walks the tree,
        // so they must be pulled out beforehand.
        extractAndNormalizeDefs(rootCopy, defRename, result, messages)

        val root = normalizeNode(rootCopy, rootName, defRename, messages)
        result[rootName] = root

        return result
    }

    // -------------------------------------------------------------------------

    private fun collectDefNames(node: JsonNode, sink: MutableSet<String>) {
        if (node !is ObjectNode) {
            if (node is ArrayNode) node.forEach { collectDefNames(it, sink) }
            return
        }
        for (key in listOf(DEFS, LEGACY_DEFS)) {
            val defs = node.get(key)
            if (defs is ObjectNode) {
                defs.fieldNames().forEach { sink.add(it) }
            }
        }
        node.fields().forEach { collectDefNames(it.value, sink) }
    }

    private fun extractAndNormalizeDefs(
        node: JsonNode,
        defRename: Map<String, String>,
        result: MutableMap<String, JsonNode>,
        messages: MutableList<String>
    ) {
        if (node !is ObjectNode) {
            if (node is ArrayNode) node.forEach { extractAndNormalizeDefs(it, defRename, result, messages) }
            return
        }
        for (key in listOf(DEFS, LEGACY_DEFS)) {
            val defs = node.get(key)
            if (defs is ObjectNode) {
                defs.fields().forEach { (name, schema) ->
                    val target = defRename[name] ?: name
                    val normalized = normalizeNode(schema.deepCopy(), target, defRename, messages)
                    result[target] = normalized
                }
            }
        }
        // Recurse into any remaining nested defs before they get stripped by normalizeNode.
        node.fields().forEach { extractAndNormalizeDefs(it.value, defRename, result, messages) }
    }

    /**
     * Recursively rewrite a single schema node in place and return it.
     */
    private fun normalizeNode(
        node: JsonNode,
        path: String,
        defRename: Map<String, String>,
        messages: MutableList<String>
    ): JsonNode {

        if (node is ArrayNode) {
            for (i in 0 until node.size()) {
                node.set(i, normalizeNode(node.get(i), path, defRename, messages))
            }
            return node
        }

        if (node !is ObjectNode) {
            return node
        }

        rewriteRef(node, defRename, messages, path)
        rewriteTypeArray(node, messages, path)
        rewriteConst(node)
        rewriteExamples(node)
        rewriteExclusiveBounds(node, messages, path)
        rewritePrefixItems(node, messages, path)
        dropUnsupported(node, messages, path)
        stripDefs(node)

        // Recurse into structural children.
        recurseChild(node, "items", path, defRename, messages)
        recurseChild(node, "additionalProperties", path, defRename, messages)
        recurseChildrenOfObject(node, "properties", path, defRename, messages)
        for (combiner in listOf("allOf", "anyOf", "oneOf")) {
            recurseChild(node, combiner, path, defRename, messages)
        }
        recurseChild(node, "not", path, defRename, messages)

        return node
    }

    private fun recurseChild(
        node: ObjectNode,
        field: String,
        path: String,
        defRename: Map<String, String>,
        messages: MutableList<String>
    ) {
        val child = node.get(field) ?: return
        // additionalProperties may be a boolean; leave scalars/booleans untouched.
        if (child.isObject || child.isArray) {
            node.set<JsonNode>(field, normalizeNode(child, "$path/$field", defRename, messages))
        }
    }

    private fun recurseChildrenOfObject(
        node: ObjectNode,
        field: String,
        path: String,
        defRename: Map<String, String>,
        messages: MutableList<String>
    ) {
        val props = node.get(field)
        if (props is ObjectNode) {
            props.fields().forEach { (name, schema) ->
                props.set<JsonNode>(name, normalizeNode(schema, "$path/$field/$name", defRename, messages))
            }
        }
    }

    // --- individual transformations -----------------------------------------

    /** `#/$defs/X` and `#/definitions/X` -> `#/components/schemas/X` (with def renaming). */
    private fun rewriteRef(node: ObjectNode, defRename: Map<String, String>, messages: MutableList<String>, path: String) {
        val ref = node.get("\$ref")?.takeIf { it.isTextual }?.asText() ?: return
        val local = when {
            ref.startsWith("#/$DEFS/") -> ref.removePrefix("#/$DEFS/")
            ref.startsWith("#/$LEGACY_DEFS/") -> ref.removePrefix("#/$LEGACY_DEFS/")
            ref.startsWith("#/components/schemas/") -> ref.removePrefix("#/components/schemas/")
            else -> {
                messages.add("Unsupported \$ref '$ref' at $path; cannot resolve external/remote references")
                return
            }
        }
        val target = defRename[local] ?: local
        node.put("\$ref", "#/components/schemas/$target")
    }

    /** `"type": [..., "null"]` -> single type + `nullable: true`; multiple non-null -> oneOf + warning. */
    private fun rewriteTypeArray(node: ObjectNode, messages: MutableList<String>, path: String) {
        val type = node.get("type")
        if (type !is ArrayNode) return

        val types = type.mapNotNull { if (it.isTextual) it.asText() else null }
        val hasNull = types.contains("null")
        val nonNull = types.filter { it != "null" }

        node.remove("type")
        if (hasNull) node.put("nullable", true)

        when {
            nonNull.isEmpty() -> {
                // only "null" — represent as a nullable string (least surprising)
                node.put("type", "string")
                messages.add("Schema at $path had type only \"null\"; treated as nullable string")
            }
            nonNull.size == 1 -> node.put("type", nonNull.first())
            else -> {
                val oneOf = node.putArray("oneOf")
                nonNull.forEach { oneOf.addObject().put("type", it) }
                messages.add("Schema at $path had multiple types $nonNull; converted to oneOf")
            }
        }
    }

    /** `const: v` -> `enum: [v]`. */
    private fun rewriteConst(node: ObjectNode) {
        val const = node.remove("const") ?: return
        node.putArray("enum").add(const)
    }

    /** `examples: [...]` -> `example: <first>` (OpenAPI 3.0 has no array form on a schema). */
    private fun rewriteExamples(node: ObjectNode) {
        val examples = node.get("examples")
        if (examples is ArrayNode && examples.size() > 0) {
            node.set<JsonNode>("example", examples.get(0))
            node.remove("examples")
        }
    }

    /**
     * JSON Schema 2020-12 uses numeric `exclusiveMinimum`/`exclusiveMaximum`.
     * OpenAPI 3.0 uses `minimum`/`maximum` plus a boolean `exclusive*` flag.
     */
    private fun rewriteExclusiveBounds(node: ObjectNode, messages: MutableList<String>, path: String) {
        for ((excl, bound) in listOf("exclusiveMinimum" to "minimum", "exclusiveMaximum" to "maximum")) {
            val v = node.get(excl) ?: continue
            if (v.isNumber) {
                node.set<JsonNode>(bound, v)
                node.put(excl, true)
            }
            // if already boolean, it's OpenAPI-3.0 form already; leave as-is
        }
    }

    /** Tuple validation `prefixItems: [...]` -> collapse to a single `items` schema (lossy). */
    private fun rewritePrefixItems(node: ObjectNode, messages: MutableList<String>, path: String) {
        val prefix = node.get("prefixItems")
        if (prefix is ArrayNode && prefix.size() > 0) {
            if (node.get("items") == null) {
                if (prefix.size() == 1) {
                    node.set<JsonNode>("items", prefix.get(0))
                } else {
                    val oneOf = mapper.createObjectNode()
                    val arr = oneOf.putArray("oneOf")
                    prefix.forEach { arr.add(it) }
                    node.set<JsonNode>("items", oneOf)
                }
            }
            node.remove("prefixItems")
            messages.add("Schema at $path used tuple 'prefixItems'; collapsed to a single 'items' schema (positional typing lost)")
        }
    }

    /** Constructs the OpenAPI 3.0 parser cannot represent: drop with a warning. */
    private fun dropUnsupported(node: ObjectNode, messages: MutableList<String>, path: String) {
        val unsupported = listOf(
            "if", "then", "else",
            "dependentSchemas", "dependentRequired",
            "patternProperties", "unevaluatedProperties", "unevaluatedItems",
            "\$dynamicRef", "\$dynamicAnchor"
        )
        for (key in unsupported) {
            if (node.has(key)) {
                node.remove(key)
                messages.add("Schema at $path used unsupported keyword '$key'; ignored")
            }
        }
    }

    /** Remove `$defs`/`definitions` after they've been lifted to siblings. */
    private fun stripDefs(node: ObjectNode) {
        node.remove(DEFS)
        node.remove(LEGACY_DEFS)
    }
}
