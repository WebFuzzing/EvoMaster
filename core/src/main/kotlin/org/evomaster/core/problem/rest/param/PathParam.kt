package org.evomaster.core.problem.rest.param

import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.gene.optional.CustomMutationRateGene


class PathParam (name: String, gene: CustomMutationRateGene<*>) : Param(name, gene){

    override fun copyContent(): Param {
        return PathParam(name, gene.copy() as CustomMutationRateGene<*>)
    }

    fun preventMutation(){
        (gene as CustomMutationRateGene<*>).preventMutation()
    }
}