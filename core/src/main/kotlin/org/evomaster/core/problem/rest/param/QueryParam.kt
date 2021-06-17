package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.Gene


class QueryParam(name: String, gene: Gene) : Param(name, gene){


    override fun copyContent(): Param {
        return QueryParam(name, gene.copyContent())
    }
}