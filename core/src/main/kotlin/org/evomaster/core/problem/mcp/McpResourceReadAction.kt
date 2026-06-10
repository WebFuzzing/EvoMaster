package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.string.StringGene

class McpResourceReadAction(
    val uri: StringGene,
    val isTemplate: Boolean = false
) : McpAction(
    id = "resource",
    parameters = mutableListOf(McpUriParam("uri", uri))
) {
    override fun getName(): String = "resource:${uri.value}"

    override fun copyContent(): Action {
        return McpResourceReadAction(uri.copy() as StringGene, isTemplate)
    }
}
