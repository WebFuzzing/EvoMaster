package org.evomaster.core.problem.api.param

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


abstract class Param(
        val name: String,
        val genes : MutableList<Gene>
) : StructuralElement(genes){

    //TODO need refactoring. eg shared abstract class for cases in which only 1 gene for sure
    @Deprecated("Assumes there is only 1 gene")
     val gene : Gene = genes[0]

    constructor(name: String, gene : Gene) : this(name, mutableListOf(gene))

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


    open fun seeGenes() : List<Gene> =  genes

    override fun copyContent(): Param {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }
}