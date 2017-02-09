package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene


class RestCallAction(
        val verb: HttpVerb,
        val path: RestPath,
        val parameters: List<out Param>,
        var auth: AuthenticationInfo = NoAuth()
) : RestAction{


    override fun copy(): Action {
        return RestCallAction(verb, path, parameters.map(Param::copy), auth)
    }

    override fun getName(): String {
        return "$verb:$path"
    }

    override fun seeGenes(): List<out Gene> {

        return parameters.map(Param::gene)
    }


}