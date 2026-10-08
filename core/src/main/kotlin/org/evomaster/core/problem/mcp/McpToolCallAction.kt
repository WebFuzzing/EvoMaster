package org.evomaster.core.problem.mcp

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene

/**
 * Action that invokes a named tool on an MCP server via the `tools/call` JSON-RPC method.
 *
 * @param toolName the name of the MCP tool to call
 * @param inputSchema an [ObjectGene] which fields represent the tool's input arguments
 */
class McpToolCallAction(
    val toolName: String,
    val inputSchema: ObjectGene
) : McpAction(
    id = "tool:$toolName",
    parameters = mutableListOf(McpInputParam("input", inputSchema))
) {
    override fun copyContent(): Action {
        return McpToolCallAction(toolName, inputSchema.copy() as ObjectGene)
    }
}
