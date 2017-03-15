package org.evomaster.experiments.unit

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class ISFIndividual(var action: ISFAction) : Individual() {


    override fun copy(): Individual {
        return ISFIndividual(action.copy() as ISFAction)
    }

    override fun seeGenes(): List<out Gene> {
        return action.seeGenes()
    }

    override fun size(): Int {
        return 1
    }

    override fun seeActions(): List<out Action> {
        return listOf(action)
    }

    override fun canMutateStructure() = true
}