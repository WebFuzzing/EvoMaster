package org.evomaster.core.search.gene.binding

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

class BindingIndividual(val genes : MutableList<Gene>) : Individual(children = genes) {

    override fun copyContent(): Individual {
        return BindingIndividual(genes.map { it.copyContent() }.toMutableList())
    }

    override fun getChildren(): List<Gene> = genes

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return genes
    }

    override fun size(): Int = genes.size

    override fun repairInitializationActions(randomness: Randomness) {
    }

    override fun seeActions(): List<out Action> = emptyList()

    override fun verifyInitializationActions(): Boolean = true
}