package org.evomaster.core.search.gene

/**
 * a gene which is primitive type, i.e., number, string
 */
interface PrimitiveGene {
    /**
     * bind value of [this] gene based on [gene]
     * @return whether the binding performs successfully
     */
    fun bindValueBasedOn(gene: Gene) : Boolean
}