package org.evomaster.core.problem.api.param

import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene


abstract class Param(
        val name: String,
        val genes : MutableList<Gene>
) : StructuralElement(genes){

    /**
     * String contains the field description from OpenAPI specification
     */
    private var description: String? = null

    fun getDescription() : String? {
        return description
    }

    fun setDescription(description: String?){
        this.description = description
    }

    //TODO need refactoring. eg shared abstract class for cases in which only 1 gene for sure
    @Deprecated("Assumes there is only 1 gene. Rather use primaryGene()")
     val gene : Gene = genes[0]

    /**
     * Return the most important gene defining this parameter.
     * This is parameter-type-dependent.
     * Note that a parameter could have more than 1 gene.
     * For example, a body param could have a gene for the object, and one for its
     * representation (e.g., JSON vs. XML)
     */
    open fun primaryGene() = genes[0]  //can be overridden if needed

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
        copy.description = description
        return copy as Param
    }


    open fun seeGenes() : List<Gene> =  genes

    override fun copyContent(): Param {
        throw IllegalStateException("${this::class.java.simpleName}: copyContent() IS NOT IMPLEMENTED")
    }
}
