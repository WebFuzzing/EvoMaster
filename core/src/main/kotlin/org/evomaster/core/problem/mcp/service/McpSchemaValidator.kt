package org.evomaster.core.problem.mcp.service

/**
 * Minimal, non-recursive JSON-Schema conformance check for MCP tool `structuredContent` against
 * a declared `outputSchema`. Only checks top-level `required` and
 * `properties[].type`; nested schemas, enums, and formats are currently out of scope.
 *
 */
object McpSchemaValidator {

    fun findViolations(schema: Map<String, Any?>, instance: Map<String, Any?>?): List<Pair<String, String>> {
        if (instance == null) {
            return listOf("missing:structuredContent" to "Tool declares an outputSchema but returned no structuredContent")
        }

        val violations = mutableListOf<Pair<String, String>>()

        val required = schema["required"] as? List<*> ?: emptyList<Any>()
        for (field in required.filterIsInstance<String>()) {
            if (!instance.containsKey(field)) {
                violations.add("missing:$field" to "Required property '$field' is absent from structuredContent")
            }
        }

        val properties = schema["properties"] as? Map<*, *> ?: emptyMap<Any, Any>()
        for ((propName, propSchema) in properties) {
            val name = propName as? String ?: continue
            if (!instance.containsKey(name)) {
                continue
            }
            val declaredType = (propSchema as? Map<*, *>)?.get("type") as? String ?: continue
            val value = instance[name]
            if (!matchesType(declaredType, value)) {
                violations.add(
                    "type_mismatch:$name" to
                        "Property '$name' expected type '$declaredType' but found value '$value'"
                )
            }
        }

        return violations
    }

    private fun matchesType(declaredType: String, value: Any?): Boolean {
        return when (declaredType) {
            "null" -> value == null
            "string" -> value is String
            "boolean" -> value is Boolean
            "integer" -> value is Int || value is Long || (value is Double && value == Math.floor(value))
            "number" -> value is Number
            "object" -> value is Map<*, *>
            "array" -> value is List<*>
            else -> true // unsupported type not flagged as error
        }
    }
}
