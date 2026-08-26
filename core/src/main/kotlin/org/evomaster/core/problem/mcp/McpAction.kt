package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.action.MainAction
import org.evomaster.core.search.gene.Gene

/**
 * Base class for all actions that can be executed against an MCP server.
 *
 * An action can be either a tool call ([McpToolCallAction]) or a resource read ([McpResourceReadAction]).
 *
 * @param id stable identifier used as the key in the action cluster
 *           and as the coverage target name (e.g. "tool:myTool", "resource:file:///data")
 * @param parameters the mutable action's inputs
 */
abstract class McpAction(
    val id: String,
    parameters: MutableList<Param>
) : MainAction(false, parameters) {

    val actionParameters: List<Param>
        get() = children as List<Param>

    override fun getName(): String = id

    override fun seeTopGenes(): List<Gene> = actionParameters.flatMap { it.seeGenes() }
}
