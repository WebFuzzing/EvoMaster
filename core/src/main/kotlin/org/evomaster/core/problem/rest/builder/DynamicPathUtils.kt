package org.evomaster.core.problem.rest.builder

import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.wrapper.OptionalGene


/**
 * POST/PUT operations can create new resources.
 * What are the ids of these newly created resources?
 * Typically, 2 options:
 * 1) returned in a HTTP Location header
 * 2) in a field of body response
 * Either way, such info is dynamically generated, and it would not
 * be known before executing the test.
 *
 * Once a test is executed, the needed info to make such a decision will be
 * stored in RestCallResult.
 */
object DynamicPathUtils {

    /**
     * Given two actions in sequence, [before] and [after], setup a creation link.
     * This means that the POST [before] is supposed to create a resource dynamically, which is then used
     * by [after].
     * eg:
     * before: POST   /products
     * after:  DELETE /products/{id}
     *
     * In case the two actions are on the same path, the [after] is linked to the creator of [before],
     * if any
     */
    fun linkDynamicCreateResource(
        before: RestCallAction,
        after: RestCallAction
    ) {
        if(before.verb != HttpVerb.POST && before.verb != HttpVerb.PUT){
            throw IllegalArgumentException("Before action is neither a POST nor a PUT. It is a ${before.verb}")
        }

        if (!before.path.isEquivalent(after.path)) {
            /*
                eg
                POST /x
                GET  /x/{id}
             */
            before.saveAndLinkLocationTo(after)
        } else {
            /*
                eg
                POST /x
                POST /x/{id}/y
                GET  /x/{id}/y
                not need to save the position of last POST, as same as target

                however, might also be in the case of:
                PUT /x/{id}
                GET /x/{id}
             */
            /*
                removing the flag here was a mistake.
                even if after is not using the resource path, between "before" and "after"
                there could be other calls that need it, eg:

                PUT    /x/{a}
                PUT    /x/{a}/y/{b}
                DELETE /x/{a}
             */
            //before.saveCreatedResourceLocation = false

            // the target (eg GET) needs to use the location of first POST, or more correctly
            // the same location used for the last POST (in case there is a deeper chain)
            after.usePreviousLocationId = before.usePreviousLocationId
            after.weakReference = before.weakReference
        }
    }


    /**
     * Check if two actions are on same resource.
     * This is not necessarily simple, as path resolution might depend on dynamic info
     * coming from previous actions (e.g., a POST create)
     */
    fun doesResolveToSamePath(a: RestCallAction, b: RestCallAction) : Boolean {

        if(!a.path.isEquivalent(b.path)){
            return false
        }

        /*
            Consider
            1) /items/{id}/{x=a}
            1) /items/{id}/{x=b}
            TODO this should result in different, even if sharing same resource {id}
         */
        if(a.usePreviousLocationId != null && a.usePreviousLocationId == b.usePreviousLocationId){
            return true
        }
        if(a.weakReference != null && a.weakReference == b.weakReference){
            return true
        }

        return a.resolvedOnlyPath() == b.resolvedOnlyPath()
    }

    /**
     * Make sure that the path params are of "this" [x] resolve to the same concrete values of "other" [y].
     * Note: "this" can be just an ancestor of "other".
     * This function takes care when path elements are dynamically handled based on
     * results of previous calls (eg a POST creating a resource).     *
     *
     **/
    fun bindToSamePathResolution(x: RestCallAction, y: RestCallAction) {
        if (!x.path.isSameOrAncestorOf(y.path)) {
            throw IllegalArgumentException("Cannot bind 2 different unrelated paths to the same path resolution: " +
                    "${x.path} vs ${y.path}")
        }
        for (i in x.parameters.indices) {
            val target = x.parameters[i]
            if (target is PathParam) {
                val k = y.parameters.find { p -> p is PathParam && p.name == target.name }!!
                /*
                    Note: even if they are referring to same path variable, it does not mean that
                    necessarily they are represented with the same type of gene, eg., typically a StringGene.
                    For example, they could be a ChoiceGene when dealing with "examples" or Regex when having patterns
                    only defined on some endpoints
                 */
                val g = x.parameters[i].primaryGene()
                g.copyValueFrom(k.primaryGene())
                g.forceNewTaints()
            }
        }
        if(x.path.isEquivalent(y.path)) {
            //if pointing to the same resource, make sure to handle dynamic resource creation
            //TODO does it make sense to do it even for ancestor paths??? likely not... but not 100% sure
            x.usePreviousLocationId = y.usePreviousLocationId
            x.weakReference = y.weakReference
        }
    }

    /**
     * When the URL path of this endpoint is resolved, would it be a (strict) parent from the other action
     */
    fun isResolvedParentPath(a: RestCallAction, b: RestCallAction): Boolean {

        val parent = a.resolvedOnlyPath() // TODO deal with dynamic info
        val child = b.resolvedOnlyPath()

        if(parent.length >= child.length) {
            return false
        }
        return child.startsWith(parent)
    }

    /**
     * Try to force [current] to use the same query params as [other], if possible.
     * Note that the 2 actions could be on DIFFERENT endpoints.
     * Equivalence is based on query param names.
     */
    fun forceSameQueryParams(current: RestCallAction, other: RestCallAction) {

        val x = current.parameters.filterIsInstance<QueryParam>()
        val y = other.parameters.filterIsInstance<QueryParam>()

        x.forEach { p ->
            val k = y.find { p.name == it.name }
            if(k == null || !k.isActive()) {
                p.primaryGene().getWrappedGene(OptionalGene::class.java)?.forbidSelection()
            } else {
                val copied = p.primaryGene().copyValueFrom(k.primaryGene())
                if(!copied){
                    p.primaryGene().getWrappedGene(OptionalGene::class.java)?.forbidSelection()
                }
            }
        }
    }
}