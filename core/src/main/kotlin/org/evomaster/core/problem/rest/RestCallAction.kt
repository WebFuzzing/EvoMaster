package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
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

    /**
     * Make sure that the path params are resolved to the same concrete values of "other".
     * Note: "this" can be just an ancestor of "other"
     */
    fun bindToSamePathResolution(other: RestCallAction){
        if(! this.path.isAncestorOf(other.path)){
            throw IllegalArgumentException("Cannot bind 2 different unrelated paths to the same path resolution: " +
                    "${this.path} vs ${other.path}")
        }

        for(i in 0 until parameters.size){
            val target = parameters[i]
            if(target is PathParam){
                val k = other.parameters.find { p -> p is PathParam && p.name == target.name }!!
                parameters[i].gene.copyValueFrom(k.gene)
            }
        }
    }
}