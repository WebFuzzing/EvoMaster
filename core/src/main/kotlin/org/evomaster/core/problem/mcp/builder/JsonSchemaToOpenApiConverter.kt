package org.evomaster.core.problem.mcp.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Converts an MCP tool `inputSchema` (JSON Schema 2020-12) into OpenAPI 3.1 Schema
 * Objects for the parser used by [org.evomaster.core.problem.rest.builder.RestActionBuilderV3].
 *
 * The result is a flat map of schema-name -> node: the root schema is keyed by [rootName], and every
 * `$defs` entry is lifted to a sibling entry so refs resolve across the set.
 */
object JsonSchemaToOpenApiConverter {

    private val mapper = ObjectMapper()

    private const val DEFS = "\$defs"

    /**
     * Convert an [inputSchema] into a flat map of OpenAPI-3.1 Schema Objects.
     *
     * @param rootName component name under which the root schema is registered
     * @param inputSchema the raw JSON Schema node (typically an MCP tool `inputSchema`)
     * @param messages list for warnings about lossy constructs
     * @return schemas keyed by component name; always contains [rootName]
     */
    fun convert(rootName: String, inputSchema: JsonNode, messages: MutableList<String>): Map<String, JsonNode> {

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
        val defs = node.get(DEFS)
        if (defs is ObjectNode) {
            defs.fieldNames().forEach { sink.add(it) }
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
        val defs = node.get(DEFS)
        if (defs is ObjectNode) {
            defs.fields().forEach { (name, schema) ->
                val target = defRename[name] ?: name
                val normalized = normalizeNode(schema.deepCopy(), target, defRename, messages)
                result[target] = normalized
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
        rewriteConst(node)
        rewritePrefixItems(node, messages, path)
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

    /** `#/$defs/X` -> `#/components/schemas/X` (with def renaming). */
    private fun rewriteRef(node: ObjectNode, defRename: Map<String, String>, messages: MutableList<String>, path: String) {
        val ref = node.get("\$ref")?.takeIf { it.isTextual }?.asText() ?: return
        val local = when {
            ref.startsWith("#/$DEFS/") -> ref.removePrefix("#/$DEFS/")
            ref.startsWith("#/components/schemas/") -> ref.removePrefix("#/components/schemas/")
            else -> {
                messages.add("Unsupported \$ref '$ref' at $path; cannot resolve external/remote references")
                return
            }
        }
        val target = defRename[local] ?: local
        node.put("\$ref", "#/components/schemas/$target")
    }

    /** `const: v` -> `enum: [v]` (the gene builder reads `enum`, not `const`). */
    private fun rewriteConst(node: ObjectNode) {
        val const = node.remove("const") ?: return
        node.putArray("enum").add(const)
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

    /** Remove `$defs` after they've been lifted to siblings. */
    private fun stripDefs(node: ObjectNode) {
        node.remove(DEFS)
    }
}
