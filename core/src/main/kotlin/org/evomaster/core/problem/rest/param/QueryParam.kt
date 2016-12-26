package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.Gene


class QueryParam(val name: String, gene: Gene) : Param(gene){

    init{
        if (name.isBlank()){
            throw IllegalArgumentException("Empty name")
        }
    }

    override fun copy(): Param {
        return QueryParam(name, gene.copy())
    }
}