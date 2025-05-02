package org.evomaster.core.problem.rest.data

import org.evomaster.core.problem.httpws.HttpWsAction
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.link.BackwardLinkReference
import org.evomaster.core.problem.rest.link.RestLink
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.ActionRToken
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.rest.util.ParserUtil
import org.evomaster.core.problem.util.BindingBuilder
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.service.Randomness
import java.net.URLEncoder


class RestCallAction(
    /**
     * An identifier for the type of this action, typically the co-ordinates verb:path.
     * but, in some special cases, we use this id to mark special type of calls
     **/
    val id:String,
    val verb: HttpVerb,
    val path: RestPath,
    parameters: MutableList<Param>,
    auth: HttpWsAuthenticationInfo = HttpWsNoAuth(),
    /**
     * If true, it means that it will
     * instruct to save the "location" header of the HTTP response for future
     * use by following calls. Typical case is to save the location of
     * a resource generated with a POST.
     * Location might inferred by returned body payload if no location header is present
     */
    saveLocation: Boolean = false,
    /**
     * Specify to use the "location" header of a
     * previous POST as path. As there might be different
     * POSTs creating different resources in the same test,
     * need to specify an id.
     *
     * Note: it might well be that we save the location returned
     * by a POST, where the POST itself might use a location for
     * path coming from a previous POST.
     *
     * It is possible that no location header is used, but id of newly created
     * resource is returned in body payload.
     * As such, we might use some heuristics to infer the "location"
     */
    var usePreviousLocationId: String? = null,
    val produces: List<String> = listOf(),
    val responseRefs : MutableMap<String, String> = mutableMapOf(),
    val skipOracleChecks : Boolean = false,
    /**
     * unique id defined in the OpenAPI schema. this is optional, though
     */
    val operationId: String? = null,
    val links: List<RestLink> = listOf(),
    var backwardLinkReference: BackwardLinkReference? = null
) : HttpWsAction(auth, parameters) {

    companion object{
        /**
         * defining potential verb for creating resources
         */
        val CONFIG_POTENTIAL_VERB_FOR_CREATION = listOf(HttpVerb.PUT, HttpVerb.POST)
    }

    var saveCreatedResourceLocation : Boolean = saveLocation
        set(value) {
            if(value && !CONFIG_POTENTIAL_VERB_FOR_CREATION.contains(verb)){
                throw IllegalArgumentException("Save location can only be used for ${CONFIG_POTENTIAL_VERB_FOR_CREATION.joinToString(",")}")
            }
            field = value
        }

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


    /**
     * @return a string representing an id to use when setting "saveLocation".
     *  following REST call can use such id to refer to the dynamically generated resource.
     */
    fun postLocationId() : String {
        if(!CONFIG_POTENTIAL_VERB_FOR_CREATION.contains(verb)){
            throw IllegalStateException("Location Ids are meaningful only for POST operations")
        }
        return  path.lastElement()
    }

    fun isPotentialActionForCreation() = CONFIG_POTENTIAL_VERB_FOR_CREATION.contains(verb)

    fun isLocationChained() = saveCreatedResourceLocation || usePreviousLocationId?.isNotBlank() ?: false

    override fun copyContent(): Action {
        val p = parameters.asSequence().map(Param::copy).toMutableList()
        return RestCallAction(
            id, verb, path, p, auth, saveCreatedResourceLocation, usePreviousLocationId,
            produces, responseRefs, skipOracleChecks, operationId, links,
            backwardLinkReference?.copy())
        //note: immutable objects (eg String) do not need to be copied
    }

    override fun getName(): String {
        return "$verb:$path"
    }

    override fun seeTopGenes(): List<out Gene> {
        return parameters.flatMap { it.seeGenes() }
    }

    override fun toString(): String {
        return "$verb ${resolvedPath()} , auth=${auth.name}"
    }

    fun resolvedPath(): String {
        return path.resolve(parameters)
    }

    fun resolvedOnlyPath() : String{
        return path.resolveOnlyPath(parameters)
    }

    /**
     * Make sure that the path params are resolved to the same concrete values of "other".
     * Note: "this" can be just an ancestor of "other"
     *
     **/
    fun bindToSamePathResolution(other: RestCallAction) {
        if (!this.path.isSameOrAncestorOf(other.path)) {
            throw IllegalArgumentException("Cannot bind 2 different unrelated paths to the same path resolution: " +
                    "${this.path} vs ${other.path}")
        }
        for (i in parameters.indices) {
            val target = parameters[i]
            if (target is PathParam) {
                val k = other.parameters.find { p -> p is PathParam && p.name == target.name }!!
                /*
                    Note: even if they are referring to same path variable, it does not mean that
                    necessarily they are represented with the same type of gene, eg., typically a StringGene.
                    For example, they could be a ChoiceGene when dealing with "examples" or Regex when having patterns
                    only defined on some endpoints
                 */
                parameters[i].primaryGene().copyValueFrom(k.primaryGene())
            }
        }
    }

    fun usingSameResolvedPath(other: RestCallAction) =
        this.resolvedOnlyPath() == other.resolvedOnlyPath()

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
     * bind [parameters] based on [other]
     * @return whether any of param is bound with [other]
     */
    fun bindBasedOn(other: RestCallAction, randomness: Randomness?) : Boolean{
        var dependent = false
        parameters.forEach { p->
            dependent = BindingBuilder.bindRestAction(p, path, other.path, other.parameters, doBuildBindingGene = true, randomness = randomness) || dependent
        }
        return dependent
    }

    /**
     * it is used to bind [this] action regarding values of [params]
     * @param otherPath is the path of the [params]
     * @param params to bind
     * @param randomness is the randomness used in binding,
     *              if the randomness is null, all params are completely bound, ie, all fields of two ObjectGene will be completely bound
     */
    fun bindBasedOn(otherPath : RestPath, params : List<Param>, randomness: Randomness?) {

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
            parameters.filterNot { param -> param is BodyParam }.forEach { param->
                BindingBuilder.bindRestAction(param, this.path, otherPath, params, randomness = randomness)
            }
        }else{
            parameters.forEach {param->
                BindingBuilder.bindRestAction(param, this.path, otherPath, params, randomness = randomness)
            }
        }
    }

    /**
     * reset [saveCreatedResourceLocation], [usePreviousLocationId] and [responseRefs] properties of [this] RestCallAction
     */
    fun resetProperties(){
        saveCreatedResourceLocation = false
        usePreviousLocationId = null
        resetLocalId()
        seeTopGenes().flatMap { it.flatView() }.forEach { it.resetLocalId() }
        clearRefs()
    }

    fun getDescription() : String? = description

    fun clearRefs(){
        responseRefs.clear()
    }
    fun addRef(key: String, ref: String){
        responseRefs[key] = ref
    }


    override fun postRandomizedChecks(randomness: Randomness?) {
        // binding params in this action, e.g., path param with body param if there exists
        BindingBuilder.bindParamsInRestAction(this, randomness = randomness)
    }

    override fun shouldSkipAssertionsOnResponseBody(): Boolean {
        return id == AbstractRestSampler.CALL_TO_SWAGGER_ID
    }

    /**
     * Check if any following action is using any link defined in this action that requires this action's HTTP
     * response results
     */
    fun hasFollowedBackwardLink() : Boolean{
        return getFollowingMainActions().any{
            val blr = (it as RestCallAction).backwardLinkReference
            blr != null
                    && blr.actualSourceActionLocalId == this.getLocalId()
                    && (this.links.find { link -> link.id == blr.sourceLinkId }?.needsToUseResponse() ?: false)
        }
    }

    fun getReferenceLinkInfo() : Pair<RestLink, RestCallAction> {
        val blr = backwardLinkReference
            ?: throw IllegalStateException("No backward link reference is defined for this action")
        if(!blr.isInUse()){
            throw IllegalStateException("Backward link reference is not in use")
        }
        val previous = getPreviousMainActions().find { it.getLocalId() == backwardLinkReference!!.actualSourceActionLocalId }
            as RestCallAction?
            ?: throw IllegalStateException("No previous action with local id ${backwardLinkReference!!.actualSourceActionLocalId}")

        val link = previous.links.find { it.id == blr.sourceLinkId }
            ?: throw IllegalStateException("No link with id ${blr.sourceLinkId} in action ${previous.id}")
        return Pair(link, previous)
    }


    fun saveAndLinkLocationTo(other: RestCallAction){
        this.saveCreatedResourceLocation = true
        other.usePreviousLocationId = this.postLocationId()
    }
}