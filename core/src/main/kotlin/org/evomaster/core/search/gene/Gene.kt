package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


/**
 * A building block representing one part of an Individual.
 * The terms "gene" comes from the evolutionary algorithm literature
 */
abstract class Gene(var name: String) {

    init{
        if(name.isBlank()){
            throw IllegalArgumentException("Empty name for Gene")
        }
    }

    abstract fun copy() : Gene

    /**
     * Specify if this gene can be mutated during the search.
     * Typically, it will be true, apart from some special cases.
     */
    open fun isMutable() = true

    /**
     * Specify if this gene should be printed in the output test.
     * In other words, if this genotype directly influences the
     * phenotype
     */
    open fun isPrintable() = true


    /**
     *   Randomize the content of this gene.
     *
     *   @param randomness the source of non-determinism
     *   @param forceNewValue whether we should force the change of value. When we do mutation,
     *          it could otherwise happen that a value is replace with itself
     *   @param allGenes if the gene depends on the other (eg a Foreign Key in SQL databases),
     *          we need to refer to them
     */
    abstract fun randomize(
            randomness: Randomness,
            forceNewValue: Boolean,
            allGenes: List<Gene> = listOf())


    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     *
     * "foo" -> "\"foo\"" -> printed as "foo"
     *
     * @param previousGenes previous genes which are necessary to look at
     * to determine the actual value of this gene
     * @param mode some genes could be printed in different ways, like an
     * object printed as JSON or XML
     */
    abstract fun getValueAsPrintableString(
            previousGenes: List<Gene> = listOf(),
            mode: String? = null
    ) : String


    open fun getValueAsRawString() = getValueAsPrintableString()

    abstract fun copyValueFrom(other: Gene)

    /**
     * If this gene represents a variable, then return its name.
     */
    open fun getVariableName() = name

    /**
     * Genes might have other genes inside (eg, think of array).
     *  @return a recursive list of all nested genes, "this" included
     */
    open fun flatView(): List<Gene>{
        return listOf(this)
    }

    /**
     * Genes might contain a value that is also stored
     * in another gene.
     */
    abstract fun containsSameValueAs(other: Gene): Boolean
}