package org.evomaster.core.problem.graphql

import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.search.action.Action
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
    auth: HttpWsAuthenticationInfo = HttpWsNoAuth()
        ) : HttpWsAction(auth, parameters) {

    override fun getName(): String {
        //TODO what if have same name but different inputs? need to add input list as well
        return "$methodName"
    }

    override fun seeTopGenes(): List<out Gene> {

        return parameters.flatMap { it.seeGenes() }
    }


    override fun copyContent(): Action {

        return GraphQLAction(id, methodName, methodType, parameters.map { it.copy() }.toMutableList(), auth)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun toString(): String {
        return "$methodType $methodName, auth=${auth.name}"
    }
}