package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.graphql.param.GraphqlParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene

class GraphqlCallAction (
        /**
         * Identifier unique within the individual
         * **/
        val id: String,
        /**
         * The name of the operation in the Query or Mutation set
         */
        val operation: String,
        val parameters:  MutableList<GraphqlParam>,
        val operationType: OperationType
) : GraphqlAction {
    override fun shouldCountForFitnessEvaluations(): Boolean = true

    override fun copy(): Action {
        val p = parameters.asSequence().map(GraphqlParam::copy).toMutableList()
        return GraphqlCallAction(id, operation, p, operationType)
    }

    override fun getName(): String {
        return "$operation"
    }

    override fun seeGenes(): List<out Gene> {

        return parameters.flatMap { it.seeGenes() }
    }

    override fun toString(): String {
        return getName()
    }
}