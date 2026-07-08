package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene

class McpToolCallAction(
    val toolName: String,
    val inputSchema: ObjectGene,
    val description: String = ""
) : McpAction(
    id = "tool:$toolName",
    parameters = mutableListOf(McpInputParam("input", inputSchema))
) {
    override fun copyContent(): Action {
        return McpToolCallAction(toolName, inputSchema.copy() as ObjectGene, description)
    }
}
