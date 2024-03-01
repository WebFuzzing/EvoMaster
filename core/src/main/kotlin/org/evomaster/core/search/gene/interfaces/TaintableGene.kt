package org.evomaster.core.search.gene.interfaces

interface TaintableGene {

    /**
     * Return the value of this gene, that "might" be tainted.
     * If for sure the value is not tainted, this method can return empty string.
     */
    fun getPossiblyTaintedValue() : String
}