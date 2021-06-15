package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.auth.AuthenticationInfo
import org.evomaster.core.problem.httpws.service.auth.NoAuth
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
        parameters: MutableList<Param>,
        auth: AuthenticationInfo = NoAuth()
        ) : HttpWsAction(auth, parameters) {

    override fun getName(): String {
        //TODO what if have same name but different inputs? need to add input list as well
        return "$methodName"
    }

    override fun seeGenes(): List<out Gene> {

        return parameters.flatMap { it.seeGenes() }
    }


    override fun copyContent(): Action {

        return GraphQLAction(id, methodName, methodType, parameters.map { it.copyContent() }.toMutableList(), auth )
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun toString(): String {
        return "$methodType $methodName, auth=${auth.name}"
    }
}