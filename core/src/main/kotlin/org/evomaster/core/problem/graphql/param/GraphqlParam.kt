package org.evomaster.core.problem.graphql.param

import org.evomaster.core.search.gene.Gene

abstract class GraphqlParam(val name: String, val gene : Gene) {

    init{
        if (name.isBlank()){
            throw IllegalArgumentException("Empty name")
        }
    }

    abstract fun copy(): GraphqlParam

    open fun seeGenes() =  listOf<Gene>(gene)
}