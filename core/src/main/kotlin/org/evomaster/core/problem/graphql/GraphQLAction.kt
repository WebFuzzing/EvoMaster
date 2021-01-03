package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.httpws.service.HttpWsAction
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
        auth: AuthenticationInfo = NoAuth()
        ) : HttpWsAction(auth) {

    override fun getName(): String {
        //TODO what if have same name but different inputs? need to add input list as well
        return "$methodName"
    }

    override fun seeGenes(): List<out Gene> {

        return parameters.flatMap { it.seeGenes() }
    }


    override fun copy(): Action {

        return GraphQLAction(id, methodName, methodType, parameters.map { it.copy() }.toMutableList(),auth )
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        TODO("Not yet implemented")
    }

}