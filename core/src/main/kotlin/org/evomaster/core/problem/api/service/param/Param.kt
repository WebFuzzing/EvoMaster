package org.evomaster.core.problem.api.service.param

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


abstract class Param(val name: String, val gene : Gene) : StructuralElement(mutableListOf(gene)){ //TODO check where children is used


    init{
        if (name.isBlank()){
            throw IllegalArgumentException("Empty name")
        }
    }

    final override fun copy(): Param {
        val copy = super.copy()
        if (copy !is Param)
            throw IllegalStateException("mismatched type: the type should be Param, but it is ${this::class.java.simpleName}")
        return copy as Param
    }


    open fun seeGenes() =  listOf(gene)

    override fun copyContent(): Param {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }
}