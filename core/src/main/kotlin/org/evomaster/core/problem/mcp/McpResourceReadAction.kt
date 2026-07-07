package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.Action

/**
 * Action to read an MCP resource.
 * For direct resources (isTemplate=false), the URI is fixed and no genes are mutable.
 * For template resources (isTemplate=true), one StringGene per URI template variable
 * (e.g. {city} in weather:///{city}/current) is created and fuzzed independently;
 * call resolvedUri() to interpolate current gene values into the template.
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
