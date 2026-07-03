package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.action.MainAction
import org.evomaster.core.search.gene.Gene

abstract class McpAction(
    val id: String,
    parameters: MutableList<Param>
) : MainAction(false, parameters) {

    val actionParameters: List<Param>
        get() = children as List<Param>

    override fun getName(): String = id

    override fun seeTopGenes(): List<Gene> = actionParameters.flatMap { it.seeGenes() }
}
