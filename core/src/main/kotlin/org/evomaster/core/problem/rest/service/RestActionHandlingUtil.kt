package org.evomaster.core.problem.rest.service

import org.evomaster.core.Lazy
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.Param
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.ResourceStatus
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.util.ParamUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Randomness

object RestActionHandlingUtil {

    /**
     * create resource for the [target]
     * @param actionCluster contains all endpoints in the sut. it can be null when resource-based mio is employed
     * @param maxTestSize is the maximum size in a test
     * @param target is target action which creates resource for
     * @param test is the test
     * @param resourceNode is the resource node of the [target]
     * @param ignoreSize specify whether to ignore maximum
     */
    fun createResourcesFor(
        randomness: Randomness,
        actionCluster: MutableMap<String, Action>?=null,
        maxTestSize: Int,
        target: RestCallAction,
        test: MutableList<RestAction>,
        resourceNode: RestResourceNode? = null
    ): ResourceStatus {

        Lazy.assert {
            // one of resourceNode and actionCluster should be null
            (resourceNode!=null).xor(actionCluster!=null)
        }

        if (test.size >= maxTestSize) {
            return ResourceStatus.NOT_ENOUGH_LENGTH
        }

        val template = chooseClosestAncestor(actionCluster, randomness, target, listOf(HttpVerb.POST), resourceNode)
            ?: return (if(target.verb == HttpVerb.POST) ResourceStatus.CREATED_REST else ResourceStatus.NOT_FOUND)

        val post = createActionFor(randomness, template, target, resourceNode!=null)

        test.add(0, post)

        /*
            Check if POST depends itself on the creation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
            (!post.path.isLastElementAParameter()) ||
            post.path.getVariableNames().size >= 2) {

            val dependencyCreated = createResourcesFor(randomness, actionCluster, maxTestSize, post, test, resourceNode)
            if (ResourceStatus.CREATED_REST != dependencyCreated) {
                return ResourceStatus.NOT_FOUND_DEPENDENT
            }
        }


        /*
            Once the POST is fully initialized, need to fix
            links with target
         */
        if (!post.path.isEquivalent(target.path)) {
            /*
                eg
                POST /x
                GET  /x/{id}
             */
            post.saveLocation = true
            target.locationId = post.path.lastElement()
        } else {
            /*
                eg
                POST /x
                POST /x/{id}/y
                GET  /x/{id}/y
             */
            //not going to save the position of last POST, as same as target
            post.saveLocation = false

            // the target (eg GET) needs to use the location of first POST, or more correctly
            // the same location used for the last POST (in case there is a deeper chain)
            target.locationId = post.locationId
        }

        return ResourceStatus.CREATED_REST
    }

    fun preventPathParamMutation(action: RestCallAction) {
        action.parameters.forEach { p -> if (p is PathParam) p.preventMutation() }
    }

    /**
     * Make sure that what returned is different from the target.
     * This can be a strict ancestor (shorter path), or same
     * endpoint but with different HTTP verb.
     * Among the different ancestors, return one of the longest
     */
    private fun chooseClosestAncestor(
        actionCluster: MutableMap<String, Action>?,
        randomness: Randomness,
        target: RestCallAction,
        verbs: List<HttpVerb>,
        resourceNode: RestResourceNode? = null
    ): RestCallAction? {

        var others = resourceNode?.sameOrAncestorEndpoints(target) ?:sameOrAncestorEndpoints(actionCluster!!, target.path)
        others = hasWithVerbs(others, verbs).filter { t -> t.getName() != target.getName() }

        if (others.isEmpty()) {
            return null
        }

        return chooseLongestPath(randomness, others)
    }

    /**
     * Get all ancestor (same path prefix) endpoints that do at least one
     * of the specified operations
     */
    private fun sameOrAncestorEndpoints(actionCluster: MutableMap<String, Action>, path: RestPath): List<RestCallAction> {
        return actionCluster.values.asSequence()
            .filter { a -> a is RestCallAction && a.path.isAncestorOf(path) }
            .map { a -> a as RestCallAction }
            .toList()
    }



    private fun chooseLongestPath(randomness: Randomness, actions: List<RestCallAction>): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.asSequence().map { a -> a.path.levels() }.max()!!
        val candidates = actions.filter { a -> a.path.levels() == max }

        return randomness.choose(candidates)
    }

    fun hasWithVerbs(actions: List<RestCallAction>, verbs: List<HttpVerb>): List<RestCallAction> {
        return actions.filter { a ->
            verbs.contains(a.verb)
        }
    }

    fun randomizeActionGenes(action: Action, randomness: Randomness, resourceNode: RestResourceNode? = null) {
        action.seeGenes().forEach { it.randomize(randomness, false) }
        if (action is RestCallAction && resourceNode != null)
            repairRandomGenes(resourceNode.path, action.parameters)
    }

    fun createActionFor(randomness: Randomness, template: RestCallAction, target: RestCallAction, employResourceBinding: Boolean = false): RestCallAction {
        val res = template.copy() as RestCallAction
        randomizeActionGenes(res, randomness)
        res.auth = target.auth
        if (employResourceBinding){
            res.bindToSamePathResolution(res.path, target.parameters)
        }else
            res.bindToSamePathResolution(target)

        return res
    }

    /********************* resource-based structure ******************************/

    private fun repairRandomGenes(path: RestPath, params : List<Param>){
        if(ParamUtil.existBodyParam(params)){
            params.filter { p -> p is BodyParam }.forEach { bp->
                ParamUtil.bindParam(bp, path, path, params.filter { p -> !(p is BodyParam)}, true)
            }
        }
        params.forEach { p->
            params.find { sp -> sp != p && p.name == sp.name && p::class.java.simpleName == sp::class.java.simpleName }?.apply {
                ParamUtil.bindParam(this, path, path, mutableListOf(p))
            }
        }
    }

    public fun isCreatedByRestAction(status : ResourceStatus) = status == ResourceStatus.CREATED_REST

}