package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene

class McpUriParam(name: String, gene: Gene) : Param(name, gene) {
    override fun copyContent(): Param = McpUriParam(name, gene.copy())
}
