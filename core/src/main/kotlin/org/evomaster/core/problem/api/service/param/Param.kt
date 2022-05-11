package org.evomaster.core.problem.api.service.param

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


abstract class Param(val name: String, val gene : Gene, children: List<StructuralElement>
) : StructuralElement(mutableListOf(gene)){ //TODO check where children is used

    constructor(name: String, gene: Gene): this(name, gene, listOf(gene))

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


    open fun seeGenes() =  listOf<Gene>(gene)

    override fun copyContent(): Param {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }
}