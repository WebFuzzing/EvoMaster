package org.evomaster.core.search.gene.binding

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

class BindingIndividual(val genes : MutableList<Gene>) : Individual(children = mutableListOf(BindingAction(genes))) {

    override fun copyContent(): Individual {
        return BindingIndividual(genes.map { it.copy() }.toMutableList())
    }


    override fun seeTopGenes(filter: ActionFilter): List<out Gene> {
        return genes
    }

    override fun size(): Int = genes.size

    override fun repairInitializationActions(randomness: Randomness) {
    }

    override fun verifyInitializationActions(): Boolean = true
}