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
        var auth: AuthenticationInfo = NoAuth(),
        /**
         * If true, it means that it will
         * instruct to save the "location" header of the HTTP response for future
         * use by following calls. Typical case is to save the location of
         * a resource generated with a POST
         */
        var saveLocation: Boolean = false,
        /**
         * Specify to use the "location" header of a
         * previous POST as path. As there might be different
         * POSTs creating different resources in the same test,
         * need to specify an id.
         *
         * Note: it might well be that we save the location returned
         * by a POST, where the POST itself might use a location for
         * path coming from a previous POST
         */
        var locationId: String? = null
) : RestAction{


    fun isLocationChained() = saveLocation || locationId?.isNotBlank() ?: false

    override fun copy(): Action {
        return RestCallAction(verb, path, parameters.map(Param::copy), auth, saveLocation, locationId)
    }

    override fun getName(): String {
        return "$verb:$path"
    }

    override fun seeGenes(): List<out Gene> {

        return parameters.map(Param::gene)
    }

    override fun toString(): String {
        return "$verb ${path.resolve(parameters)} , auth=${auth.name}"
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