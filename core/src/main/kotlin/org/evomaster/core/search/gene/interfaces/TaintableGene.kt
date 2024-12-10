package org.evomaster.core.search.gene.interfaces

interface TaintableGene {

    /**
     * Return the value of this gene, that "might" be tainted.
     * If for sure the value is not tainted, this method can return empty string.
     */
    fun getPossiblyTaintedValue() : String

    /**
     * After taint analysis, genotype can be modified to enable to use what learned in the next mutations.
     * This info is represented by "dormant" genes, that need to be manually activated before a new mutation.
     */
    fun hasDormantGenes() : Boolean

    /**
     * Make sure the taint id for this gene, if any is in place, is replaced with a new unique one.
     * This is needed for example when copying and pasting actions, to avoid having same id repeated
     * more than once.
     *
     * //TODO once properly implemented, should have check unique id as invariant in Individual
     */
    fun forceNewTaintId()

    //TODO evolve()
}