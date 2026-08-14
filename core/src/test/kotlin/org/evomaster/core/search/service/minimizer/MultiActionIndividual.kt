package org.evomaster.core.search.service.minimizer

import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

//unlike OneMaxIndividual/ConstantIndividual, size() is not hardcoded to 1, as Minimizer needs
//individuals with a variable number of main-executable actions
class MultiActionIndividual(actions: List<MultiActionAction>) : Individual(children = actions.toMutableList()) {

    override fun copyContent(): Individual {
        return MultiActionIndividual(children.map { it.copy() as MultiActionAction })
    }

    override fun seeActions(filter: ActionFilter): List<Action> {
        return when (filter) {
            ActionFilter.ALL, ActionFilter.NO_INIT, ActionFilter.MAIN_EXECUTABLE -> seeAllActions()
            else -> listOf()
        }
    }

    override fun seeMainExecutableActions(): List<MultiActionAction> {
        @Suppress("UNCHECKED_CAST")
        return super.seeMainExecutableActions() as List<MultiActionAction>
    }

    override fun seeTopGenes(filter: ActionFilter): List<Gene> {
        return listOf()
    }

    override fun size(): Int {
        return children.size
    }

    override fun isValidInitializationActions(errors: MutableList<String>?): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {
        //no-op
    }
}
