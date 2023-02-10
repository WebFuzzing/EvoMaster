package org.evomaster.core.problem.rest.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.OptionalGene


class HeaderParam(name: String, gene: Gene) : Param(name, gene){

    override fun copyContent(): Param {
        return HeaderParam(name, gene.copy())
    }

    fun isInUse() = gene !is OptionalGene || gene.isActive
}