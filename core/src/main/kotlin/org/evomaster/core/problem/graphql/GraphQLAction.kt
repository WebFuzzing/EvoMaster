package org.evomaster.core.problem.graphql

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene


class GraphQLAction(val id:String,
                    var tableName:String?,
                    var tableField:String?,
                    var tableType:String?) : Action{

    override fun getName(): String {

        return "$tableName$tableField"
    }

    override fun seeGenes(): List<out Gene> {
        TODO("Not yet implemented")
    }


    override fun copy(): Action {
        TODO("Not yet implemented")
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        TODO("Not yet implemented")
    }

}