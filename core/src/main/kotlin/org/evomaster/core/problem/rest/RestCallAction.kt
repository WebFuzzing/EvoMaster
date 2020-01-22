package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.resource.ActionRToken
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.problem.rest.util.ParserUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.OptionalGene
import java.net.URLEncoder


class RestCallAction(
        /**
         * Identifier unique within the individual
         * **/
        val id:String,
        val verb: HttpVerb,
        val path: RestPath,
        val parameters: MutableList<Param>,
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
        var locationId: String? = null,
        val produces: List<String> = listOf(),
        val responseRefs : MutableMap<String, String> = mutableMapOf()
) : RestAction {

    /**
     * collect info of description and summary from swagger
     */
    private var description : String? = null

    /**
     * tokens is used to present a set of tokens exist in the scheme, e.g., swagger specification
     * key is a parsed token
     * value is [ActionRToken]
     */
    val tokens : MutableMap<String, ActionRToken> = mutableMapOf()


    override fun shouldCountForFitnessEvaluations(): Boolean = true

    fun isLocationChained() = saveLocation || locationId?.isNotBlank() ?: false

    override fun copy(): Action {
        val p = parameters.asSequence().map(Param::copy).toMutableList()
        return RestCallAction(id, verb, path, p, auth, saveLocation, locationId, produces, responseRefs)
    }

    override fun getName(): String {
        return "$verb:$path"
    }

    override fun seeGenes(): List<out Gene> {
        return parameters.flatMap { it.seeGenes() }
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
     *
     * Man: extend bind other types of params, e.g., body param
     **/
    fun bindToSamePathResolution(other: RestCallAction) {
        if (!this.path.isAncestorOf(other.path)) {
            throw IllegalArgumentException("Cannot bind 2 different unrelated paths to the same path resolution: " +
                    "${this.path} vs ${other.path}")
        }
//        for (i in 0 until parameters.size) {
//            val target = parameters[i]
//            if (target is PathParam) {
//                val k = other.parameters.find { p -> p is PathParam && p.name == target.name }!!
//                parameters[i].gene.copyValueFrom(k.gene)
//            }
//        }
        bindToSamePathResolution(other.path, other.parameters)
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
    @Deprecated("No needed anymore with OpenAPI v3")
    fun getBodyFormData(): String? {

        val forms = parameters.filterIsInstance<FormParam>()
        if(forms.isEmpty()){
            return null
        }

        return forms.filter { it.gene !is OptionalGene || it.gene.isActive }
                .map {
                    val name = URLEncoder.encode(it.gene.getVariableName(), "UTF-8")
                    val value = URLEncoder.encode(it.gene.getValueAsRawString(), "UTF-8")
                    "$name=$value"
                }
                .joinToString("&")
    }

    fun initTokens(description : String?){
        if(description!= null){
            this.description = description
            tokens.clear()
            ParserUtil.parseAction(this, description, tokens)
        }
    }

    /**
     * it is used to bind [this] action regarding values of [params]
     */
    fun bindToSamePathResolution(otherPath : RestPath, params : List<Param>) {

        if(params.isEmpty()){
            //no param is required to bind
            return
        }
        /*
           there may exist that a rest action has e.g., path parameter and body parameter.
           in this case (not all body param), we only bind non- BodyParam, e.g., PathParam
           the body parameter will be bound by "repair" process to ensure the same attribute of path and body parameter have same value.
         */
        if(!ParamUtil.isAllBodyParam(parameters)){
            parameters.filter { param -> !(param is BodyParam) }.forEach { param->
                ParamUtil.bindParam(param, this.path, otherPath, params)
            }
        }else{
            parameters.forEach {param->
                ParamUtil.bindParam(param, this.path, otherPath, params)
            }
        }
    }

    fun getDescription() : String? = description
    fun clearRefs(){
        responseRefs.clear()
    }
    fun addRef(key: String, ref: String){
        responseRefs[key] = ref
    }

}