package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene


class GraphQLAction(
        /**
         * A unique id to identify this action
         */
        val id: String,
        /**
         * the name of the Query or Mutation in the schema
         */
        val methodName: String,
        val methodType: GQMethodType,
        val parameters: MutableList<Param>,
        var auth: AuthenticationInfo = NoAuth()
        ) : Action{

    override fun getName(): String {
        //TODO what if have same name but different inputs? need to add input list as well
        return "$methodName"
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