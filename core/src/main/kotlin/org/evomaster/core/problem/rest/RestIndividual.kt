package org.evomaster.core.problem.rest

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene


class RestIndividual(val actions: MutableList<RestAction>) : Individual() {

    override fun copy(): Individual {
        return RestIndividual(actions.map { a -> a.copy() as RestAction } as MutableList<RestAction>)
    }

    override fun seeGenes(): List<out Gene> {

        return actions.flatMap(RestAction::seeGenes)
    }

    override fun size() = actions.size

    override fun seeActions(): List<out Action> {
        return actions
    }

}