package org.evomaster.core.problem.graphql

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.TrackOperator

class GraphQLIndividual(
        val actions: MutableList<out Action>,
        val sampleType: SampleType,
        val dbInitialization: MutableList<DbAction> = mutableListOf()
) : Individual() {

    override fun copy(): Individual {
        TODO("Not yet implemented")
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        TODO("Not yet implemented")
    }

    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun seeActions(): List<out Action> {
        TODO("Not yet implemented")
    }

    override fun verifyInitializationActions(): Boolean {
        TODO("Not yet implemented")
    }

    override fun repairInitializationActions(randomness: Randomness) {
        TODO("Not yet implemented")
    }

}