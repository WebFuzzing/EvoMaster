package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.Action

/**
 * Action that reads a resource from an MCP server via the `resources/read` JSON-RPC method.
 *
 * @param uriTemplate the resource URI or URI template string (e.g. `file:///data`, `weather:///{city}/current`)
 * @param uriParams one [McpUriParam] per template variable; empty for direct resources
 * @param isTemplate whether this action represents a URI template or a fixed URI
 */
class McpResourceReadAction(
    val uriTemplate: String,
    val uriParams: List<McpUriParam>,
    val isTemplate: Boolean = false
) : McpAction(
    id = "resource",
    parameters = uriParams.toMutableList()
) {
    override fun getName(): String = "resource:$uriTemplate"

    fun resolvedUri(): String {
        if (!isTemplate) return uriTemplate
        var result = uriTemplate
        uriParams.forEach { param ->
            result = result.replace("{${param.name}}", param.primaryGene().getValueAsRawString())
        }
        return result
    }

    override fun copyContent(): Action =
        McpResourceReadAction(uriTemplate, uriParams.map { it.copy() as McpUriParam }, isTemplate)
}
