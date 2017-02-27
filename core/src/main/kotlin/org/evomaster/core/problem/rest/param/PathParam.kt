package org.evomaster.core.problem.rest.param

import org.evomaster.core.search.gene.DisruptiveGene


class PathParam (name: String, gene: DisruptiveGene<*>) : Param(name, gene){

    override fun copy(): Param {
        return PathParam(name, gene.copy() as DisruptiveGene<*>)
    }

    fun preventMutation(){
        (gene as DisruptiveGene<*>).probability = 0.0
    }
}