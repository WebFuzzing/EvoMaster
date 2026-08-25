package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

/** Parameter representing a single URI template variable in a [McpResourceReadAction]. */
class McpUriParam(name: String, gene: Gene) : Param(name, gene) {
    override fun copyContent(): Param = McpUriParam(name, gene.copy())
}
