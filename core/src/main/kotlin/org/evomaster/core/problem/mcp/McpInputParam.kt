package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

/** Parameter that carries the payload of a [McpToolCallAction]. */
class McpInputParam(name: String, gene: Gene) : Param(name, gene) {
    override fun copyContent(): Param = McpInputParam(name, gene.copy())
}
