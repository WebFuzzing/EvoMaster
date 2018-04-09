package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene
import java.net.URLEncoder


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
) : RestAction {

    override fun shouldCountForFitnessEvaluations(): Boolean = true

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
        return "$verb ${resolvedPath()} , auth=${auth.name}"
    }

    fun resolvedPath(): String {
        return path.resolve(parameters)
    }

    /**
     * Make sure that the path params are resolved to the same concrete values of "other".
     * Note: "this" can be just an ancestor of "other"
     */
    fun bindToSamePathResolution(other: RestCallAction) {
        if (!this.path.isAncestorOf(other.path)) {
            throw IllegalArgumentException("Cannot bind 2 different unrelated paths to the same path resolution: " +
                    "${this.path} vs ${other.path}")
        }

        for (i in 0 until parameters.size) {
            val target = parameters[i]
            if (target is PathParam) {
                val k = other.parameters.find { p -> p is PathParam && p.name == target.name }!!
                parameters[i].gene.copyValueFrom(k.gene)
            }
        }
    }

    /**
    Note: in swagger the "consume" type might be missing.
    So, if for any reason there is a form param, then consider
    the body as an application/x-www-form-urlencoded

    see https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1
    Note: that is old HTML 4, still dealing with RFC 1738, from 1994...

    HTML 5.1 (November 2016) has these rules:
    https://www.w3.org/TR/html/sec-forms.html#urlencoded-form-data

    which unfortunately are unreadable...

    Regarding URLEncoder in Java 8, it refers to URIs from RFC 2396 from
    1998 (updating RFC 1738), which is obsoleted by RFC 3986 since 2005!!!

    Plus, x-www-form-urlencoded and encoding of URIs are not the same!!!

    REALLY: WTF?!?

    TODO: update/verify based on
    https://url.spec.whatwg.org/#concept-urlencoded-byte-serializer

     */
    fun getBodyFormData(): String {
        return parameters.filter { p -> p is FormParam }
                .filter { p -> p.gene !is OptionalGene || p.gene.isActive }
                .map { p ->
                    val name = URLEncoder.encode(p.gene.getVariableName(), "UTF-8")
                    val value = URLEncoder.encode(p.gene.getValueAsRawString(), "UTF-8")
                    "$name=$value"
                }
                .joinToString("&")
    }
}