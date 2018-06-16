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


    // FIXME: refactoring by adding "allGenes: List<Gene> = listOf()"

    abstract fun randomize(randomness: Randomness, forceNewValue: Boolean)

    /**
     * Return the value as a printable string.
     * Once printed, it would be equivalent to the actual value, eg
     *
     * 1 -> "1" -> printed as 1
     *
     * "foo" -> "\"foo\"" -> printed as "foo"
     */
    abstract fun getValueAsPrintableString() : String

    //FIXME refactor below method into above

    /**
     * @param previousGenes previous genes which are necessary to look at
     * to determine the actual value of this gene
     */
    open fun getValueAsPrintableString(previousGenes: List<Gene>) : String {
        return getValueAsPrintableString()
    }

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
}