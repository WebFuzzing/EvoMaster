package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.service.Randomness
import javax.ws.rs.POST


/**
 * Set of operations to help creating new individuals, taking into account what is available
 * in the schema (but NOT the archive).
 */
class RestIndividualBuilder {

    @Inject
    private lateinit var sampler: AbstractRestSampler

    @Inject
    private lateinit var randomness: Randomness

    companion object{

        fun sliceAllCallsInIndividualAfterAction(
            evaluatedIndividual: EvaluatedIndividual<RestIndividual>,
            verb: HttpVerb? = null,
            path: RestPath? = null,
            status: Int? = null,
            statusGroup: StatusGroup? = null,
            statusCodes: List<Int>? = null,
            authenticated: Boolean? = null,
            authenticatedWith: String? = null
        ) : RestIndividual {

            val index = RestIndividualSelectorUtils.findIndexOfAction(
                evaluatedIndividual, verb, path, status, statusGroup, statusCodes, authenticated, authenticatedWith)

            return sliceAllCallsInIndividualAfterAction(evaluatedIndividual.individual, index)
        }


        /**
         * Create a copy of [restIndividual], where all main actions after index are removed
         */
        fun sliceAllCallsInIndividualAfterAction(restIndividual: RestIndividual, actionIndex: Int) : RestIndividual {

            // we need to check that the index is within the range
            if (actionIndex < 0 || actionIndex > restIndividual.size() -1) {
                throw IllegalArgumentException("Action index has to be between 0 and ${restIndividual.size()}")
            }

            val ind = restIndividual.copy() as RestIndividual

            val n = ind.seeMainExecutableActions().size

            /*
                We start from last, going backward.
                So, actionIndex stays the same
             */
            for(i in n-1 downTo actionIndex+1){
                ind.removeMainExecutableAction(i)
            }

            ind.fixGeneBindingsIfNeeded()
            ind.fixResourceForwardLinks()

            return ind
        }


        fun merge(first: RestIndividual, second: RestIndividual, third: RestIndividual): RestIndividual {
            return merge(merge(first, second), third)
        }

        /**
         * Create a new individual, based on [first] followed by [second].
         * Initialization actions are properly taken care of.
         */
        fun merge(first: RestIndividual, second: RestIndividual): RestIndividual {

            val before = first.seeAllActions().size + second.seeAllActions().size

            val base = first.copy() as RestIndividual
            base.ensureFlattenedStructure()
            val other = second.copy() as RestIndividual
            other.ensureFlattenedStructure()

            /*
                we need to reset local ids in other, to avoid clashes with ids in base.
                however, need to make sure no chain is broken
             */
            other.seeAllActions().filterIsInstance<RestCallAction>()
                .forEach { it.revertToWeakReference() }
            other.resetLocalIdRecursively()

            val duplicates = base.addInitializingActions(other.seeInitializingActions())

            other.getFlattenMainEnterpriseActionGroup()!!.forEach { group ->
                base.addMainEnterpriseActionGroup(group)
            }

            base.resolveAllTempData()

            /*
                TODO are links properly handled in such a merge???
                would need assertions here, as well as test cases
             */

            val after = base.seeAllActions().size
            //merge shouldn't lose any actions
            assert(before == (after+duplicates)) { "$after+$duplicates!=$before" }

            base.verifyValidity()

            return base
        }
    }


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

        //TODO might need to relax this constraint
        if (!template.path.isSameOrAncestorOf(target.path)) {
            throw IllegalArgumentException("Cannot create an action for unrelated paths: " +
                    "${template.path} vs ${target.path}")
        }

        val res = template.copyKeepingSameWeakRef()

        res.resetLocalIdRecursively()

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
     *  Based on given [template], create new action that is bound to a previous operation.
     *  For example:
     *  template: DELETE /users/{id}
     *  previous: POST   /users
     *  would lead to return something like:
     *            DELETE /users/42
     */
    fun createBoundActionOnPreviousCreate(template: RestCallAction, previous: RestCallAction): RestCallAction {

        if(previous.verb != HttpVerb.POST && previous.verb != HttpVerb.PUT) {
            throw IllegalArgumentException("'previous' is not a create operation: ${previous.verb}")
        }

        //We have relaxed this constraint to be able to handle real-world APIs
//        if (!previous.path.isSameOrAncestorOf(template.path)) {
//            throw IllegalArgumentException("Cannot create an action for unrelated paths: " +
//                    "${previous.path} vs ${template.path}")
//        }

        val res = template.copyKeepingSameWeakRef()

        res.resetLocalIdRecursively()

        if(res.isInitialized()){
            res.seeTopGenes().forEach { it.randomize(randomness, false) }
        } else {
            res.doInitialize(randomness)
        }
        res.auth = previous.auth
        if(res.path.isEquivalent(previous.path)) {
            res.bindToSamePathResolution(previous)
        }
        CreateResourceUtils.linkDynamicCreateResource(previous, res)

        return res
    }


    /**
     * Make sure that what returned is different from the target.
     * This can be a strict ancestor (shorter path), or same
     * endpoint but with different HTTP verb.
     * Among the different ancestors, return one of the longest
     *
     * @return a potentially null [RestCallAction] using any of the given verbs,
     *        and that is different from [target], but having same or ancestor path.
     */
    fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>): RestCallAction? {

        var others = sameOrAncestorEndpoints(target.path)
        others = filterBasedOnVerbs(others, verbs)
            .filter {
                //recall name is using verb and path
                t -> t.getName() != target.getName()
            }

        if (others.isEmpty()) {
            return null
        }

        return chooseLongestPath(others)
    }

    /**
     * return all actions using any of the specified verbs
     */
    private fun filterBasedOnVerbs(actions: List<RestCallAction>, verbs: List<HttpVerb>): List<RestCallAction> {
        return actions.filter { a ->
            verbs.contains(a.verb)
        }
    }

    /**
     * Get all ancestor (or same path) endpoints.
     * This is based on their path structure
     */
    fun sameOrAncestorEndpoints(path: RestPath): List<RestCallAction> {
        return sampler.seeAvailableActions().asSequence()
            .filterIsInstance<RestCallAction>()
            .filter { it.path.isSameOrAncestorOf(path) }
            .toList()
    }

    private fun findTemplate(path: RestPath, verb: HttpVerb) : RestCallAction? {
        return sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()
            .find { it.path.isEquivalent(path) && it.verb == verb }
    }

    private fun chooseLongestPath(actions: List<RestCallAction>): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.asSequence().map { a -> a.path.levels() }.maxOrNull()!!
        val candidates = actions.filter { a -> a.path.levels() == max }

        return randomness.choose(candidates)
    }

    /**
     * Create a new action, to be added to the list representing the [test] we are building.
     * Given a [target] (eg, a GET on a specific resource path X), create a new action that should lead
     * to the creation of such resource.
     * Target must be inside [test].
     * This new action will be added at the beginning of [test].
     * As such action might need its own ancestor resources, this process is then applied recursively.
     */
     fun createResourcesFor(target: RestCallAction, test: MutableList<RestCallAction>)
            : Boolean {

         if(!test.contains(target)){
             throw IllegalArgumentException("Target ${target.getName()} is not inside test:" +
                     " ${test.joinToString(" , ") { it.getName() }}")
         }

        val postTemplate = chooseClosestAncestor(target, listOf(HttpVerb.POST))
        val putTemplate = if(target.verb != HttpVerb.PUT) findTemplate(target.path, HttpVerb.PUT) else null

        if(postTemplate == null && putTemplate == null) {
            return false
        }
        val template : RestCallAction = if(putTemplate == null){
            postTemplate!!
        } else if(postTemplate == null){
            putTemplate
        } else {
           if(randomness.nextBoolean(0.8)){
               //prefer POST if both are available
               postTemplate
           } else {
               putTemplate
           }
        }

        if(test.filter { it.path == template.path && it.verb == template.verb}.isNotEmpty()){
            //we already have a resource creation for this path
            return false
        }

        val create = createBoundActionFor(template, target)

        if(template.verb == HttpVerb.PUT){
            /*
                TODO: should check if body payload has any id matching the path element...
                if so, should bind it
             */
        }

        test.add(0, create)

        /*
            Check if create action depends itself on the creation of
            some intermediate resources
         */
        if (
            (create.path.hasVariablePathParameters() && !create.path.isLastElementAParameter())
            || create.path.getVariableNames().size >= 2
            ) {

            val dependencyCreated = createResourcesFor(create, test)
            if (!dependencyCreated) {
                return false
            }
        }


        /*
            Once the create is fully initialized, need to fix
            links with target
         */
        CreateResourceUtils.linkDynamicCreateResource(create, target)

        return true
    }

}
