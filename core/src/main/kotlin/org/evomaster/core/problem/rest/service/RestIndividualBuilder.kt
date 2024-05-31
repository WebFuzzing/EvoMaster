package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.service.Randomness


class RestIndividualBuilder {

    @Inject
    private lateinit var sampler: AbstractRestSampler

    @Inject
    private lateinit var randomness: Randomness


    /**
     * Based on a given [template], create a new action for it.
     * Such new action will have the same path resolution of [target], using same auth.
     * Note that [template] must have same path of be an ancestor of [target].
     * For example:
     * template: GET /users/{id}
     * target:   PUT /users/42/orders/77
     * would lead to return something like:
     *           GET /users/42
     *
     */
    fun createBoundActionFor(template: RestCallAction, target: RestCallAction): RestCallAction {

        if (!template.path.isSameOrAncestorOf(target.path)) {
            throw IllegalArgumentException("Cannot create an action for unrelated paths: " +
                    "${template.path} vs ${target.path}")
        }

        val res = template.copy() as RestCallAction
        if(res.isInitialized()){
            res.seeTopGenes().forEach { it.randomize(randomness, false) }
        } else {
            res.doInitialize(randomness)
        }
        res.auth = target.auth
        res.bindToSamePathResolution(target)

        return res
    }

    /**
     * Make sure that what returned is different from the target.
     * This can be a strict ancestor (shorter path), or same
     * endpoint but with different HTTP verb.
     * Among the different ancestors, return one of the longest
     */
    fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>): RestCallAction? {

        var others = sameOrAncestorEndpoints(target.path)
        others = hasWithVerbs(others, verbs)
            .filter { t -> t.getName() != target.getName() }

        if (others.isEmpty()) {
            return null
        }

        return chooseLongestPath(others)
    }

    private fun hasWithVerbs(actions: List<RestCallAction>, verbs: List<HttpVerb>): List<RestCallAction> {
        return actions.filter { a ->
            verbs.contains(a.verb)
        }
    }

    /**
     * Get all ancestor (same path prefix) endpoints
     */
    fun sameOrAncestorEndpoints(path: RestPath): List<RestCallAction> {
        return sampler.seeAvailableActions().asSequence()
            .filterIsInstance<RestCallAction>()
            .filter { it.path.isSameOrAncestorOf(path) }
            .toList()
    }

    private fun chooseLongestPath(actions: List<RestCallAction>): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.asSequence().map { a -> a.path.levels() }.maxOrNull()!!
        val candidates = actions.filter { a -> a.path.levels() == max }

        return randomness.choose(candidates)
    }

     fun createResourcesFor(target: RestCallAction, test: MutableList<RestCallAction>)
            : Boolean {


        val template = chooseClosestAncestor(target, listOf(HttpVerb.POST))
            ?: return false

        val post = createBoundActionFor(template, target)

        test.add(0, post)

        /*
            Check if POST depends itself on the creation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
            (!post.path.isLastElementAParameter()) ||
            post.path.getVariableNames().size >= 2) {

            val dependencyCreated = createResourcesFor(post, test)
            if (!dependencyCreated) {
                return false
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

        return true
    }


    /**
     * Create a copy of individual, where all main actions after index are removed
     */
    fun sliceAllCallsInIndividualAfterAction(restIndividual: RestIndividual, actionIndex: Int) : RestIndividual {

        //TODO move code here
        return RestIndividualSelectorUtils.sliceAllCallsInIndividualAfterAction(restIndividual, actionIndex)
    }
}