package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionFilter
import org.evomaster.core.search.GeneFilter
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness

class GraphQLIndividual(
        val actions: MutableList<out Action>,
        val sampleType: SampleType,
        val dbInitialization: MutableList<DbAction> = mutableListOf()
) : Individual() {

    override fun copy(): Individual {

        return GraphQLIndividual(
                actions.map { it.copy() }.toMutableList(),
                sampleType,
                dbInitialization.map { it.copy() as DbAction } as MutableList<DbAction>
        )

    }


    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return when (filter) {
            GeneFilter.ALL -> dbInitialization.flatMap(DbAction::seeGenes).plus(seeActions().flatMap(Action::seeGenes))
            GeneFilter.NO_SQL -> seeActions().flatMap(Action::seeGenes)
            GeneFilter.ONLY_SQL -> dbInitialization.flatMap(DbAction::seeGenes)
            else -> throw IllegalArgumentException("$filter is not supported for GraphQLIndividual")
        }
    }

    override fun size(): Int {
        return seeActions().size
    }

    override fun seeActions(filter: ActionFilter): List<out Action> {
        return actions
    }

    override fun verifyInitializationActions(): Boolean {
        return DbActionUtils.verifyActions(dbInitialization.filterIsInstance<DbAction>())
    }

    override fun repairInitializationActions(randomness: Randomness) {
        TODO("Not yet implemented")
    }


}