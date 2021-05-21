package org.evomaster.core.search.gene

import org.evomaster.core.search.Individual

/**
 * a gene is able to bind its value with others, i.e., number, string
 * For instance,
 *      for the gene with structure, such as Object, we do not recognize it as BindableGene,
 *      but its fields can be bindable gene
 */
interface ValueBindableGene {


    /**
     * bind value of [this] gene based on [gene]
     * @return whether the binding performs successfully
     */
    fun bindValueBasedOn(gene: Gene) : Boolean

}