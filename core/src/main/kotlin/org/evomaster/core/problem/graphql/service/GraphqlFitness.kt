package org.evomaster.core.problem.graphql.service

import org.evomaster.core.problem.graphql.GraphqlIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction

class GraphqlFitness : FitnessFunction<GraphqlIndividual>() {
    override fun doCalculateCoverage(individual: GraphqlIndividual): EvaluatedIndividual<GraphqlIndividual>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates..
        return null
    }

}