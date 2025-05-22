package org.evomaster.core.problem.rest.builder


import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.problem.rest.service.RestIndividualBuilder
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.ActionResult


/**
 * Utility functions to select one or more REST Individual from a group, based on different criteria.
 * Or to select specific actions.
 */
object RestIndividualSelectorUtils {

    /**
     * check a given action for the given conditions
     */
    private fun checkIfActionSatisfiesConditions(act: EvaluatedAction,
                                                 verb: HttpVerb? = null,
                                                 path: RestPath? = null,
                                                 status: Int? = null,
                                                 statusGroup: StatusGroup? = null,
                                                 statusCodes: Collection<Int>? = null,
                                                 authenticated : Boolean? = null,
                                                 authenticatedWith: String? = null
                                         ) : Boolean {

        // get actions and results first
        val action = act.action as RestCallAction
        val resultOfAction = act.result as RestCallResult

        // now check all conditions to see if any of them make the action not be included

        // verb
        if (verb != null && action.verb != verb) {
                return false
        }

        // path
        if(path != null && !action.path.isEquivalent(path)) {
            return false
        }

        // status
        if(status!=null && resultOfAction.getStatusCode() != status){
            return false
        }

        if(statusGroup != null && !statusGroup.isInGroup(resultOfAction.getStatusCode())){
            return false
        }

        if(!statusCodes.isNullOrEmpty() && !statusCodes.contains(resultOfAction.getStatusCode())) {
            return false
        }

        if(authenticated != null) {
            // authenticated or not
            if (authenticated == true && action.auth is NoAuth) {
                return false
            }
            if(authenticated == false && action.auth !is NoAuth) {
                return false
            }
        }

        if(authenticatedWith != null && action.auth.name != authenticatedWith){
            return false
        }

        return true
    }

    fun findAction(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        authenticated: Boolean? = null
    ): RestCallAction? {

        return findEvaluatedAction(individualsInSolution,verb,path,status,statusGroup,authenticated)
            ?.action as RestCallAction?
    }


    fun findEvaluatedAction(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        authenticated: Boolean? = null
    ): EvaluatedAction? {

        val actions = findEvaluatedActions(individualsInSolution,verb,path,status,statusGroup,authenticated)

        return if(actions.isEmpty()) {
            null
        } else {
            actions[0]
        }
    }

    fun findActionsInIndividual(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        authenticated: Boolean? = null
    ): List<EvaluatedAction> {

        if(status != null && statusGroup!= null){
            throw IllegalArgumentException("Shouldn't specify both status and status group")
        }

        return individual.seeMainExecutableActions()
            .mapNotNull { a ->
                val res = actionResults.find { r ->  r.sourceLocalId == a.getLocalId() }
                if(res == null){
                    //recall, test execution might had been stopped
                    null
                } else {
                    EvaluatedAction(a,res)
                }
            }
            .filter { a ->
                checkIfActionSatisfiesConditions(a, verb, path, status, statusGroup, null, authenticated)
            }
    }

    fun findEvaluatedActions(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        authenticated: Boolean? = null
    ): List<EvaluatedAction> {

        if(status != null && statusGroup!= null){
            throw IllegalArgumentException("Shouldn't specify both status and status group")
        }

        return individualsInSolution.flatMap {ind ->
            ind.evaluatedMainActions().filter { a ->
                checkIfActionSatisfiesConditions(a, verb, path, status, statusGroup, null, authenticated)
            }
        }
    }


    /**
     * Find individuals which contain actions with given parameters, such as verb, path, result.
     * If any of those parameters are not given as null, that parameter is used for filtering individuals.
     */
    fun findIndividuals(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        statusCodes: Collection<Int>? = null,
        authenticated: Boolean? = null,
        authenticatedWith: String? = null
    ): List<EvaluatedIndividual<RestIndividual>> {

        if(status != null && statusGroup!= null){
            throw IllegalArgumentException("Shouldn't specify both status and status group")
        }
        if(authenticated == false && authenticatedWith != null){
            throw IllegalArgumentException("Cannot specify no authentication but then ask for a specific authenticated user")
        }

        return individualsInSolution.filter {ind ->
            ind.evaluatedMainActions().any { a ->
                checkIfActionSatisfiesConditions(a, verb, path, status, statusGroup, statusCodes, authenticated, authenticatedWith)
            }
        }
    }

    /**
     * Find all individuals with an action having given properties.
     * Return a slice, where all actions after the target are removed.
     */
    fun findAndSlice(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        statusCodes: Collection<Int>? = null,
        authenticated: Boolean? = null,
        authenticatedWith: String? = null
    ) : List<RestIndividual>{

        val individuals = findIndividuals(individualsInSolution, verb, path, status, statusGroup, statusCodes, authenticated, authenticatedWith)

        return individuals.map { ind ->
            val index = findIndexOfAction(ind, verb, path, status, statusGroup, statusCodes, authenticated, authenticatedWith)
            RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(ind.individual, index)
        }
    }


    /**
     * get all action definitions from the swagger based on the given verb
     */
    fun getAllActionDefinitions(actionDefinitions: List<RestCallAction>, verb: HttpVerb): List<RestCallAction> {
        return actionDefinitions.filter { it.verb == verb }
    }

    /**
     * Given a resource path, we want to find any individual that has a successful create operation for that resource
     *
     * Assume for example the resource "/users/{id}"
     *
     * We could search for a
     * PUT "/users/id"
     * that returns 201
     *
     * or a
     * POST "/users"
     * that returns something in 2xx (not necessarily 201)
     *
     * @return null if none found
     */
    fun findIndividualWithEndpointCreationForResource(individuals: List<EvaluatedIndividual<RestIndividual>>,
                                                      resourcePath: RestPath,
                                                      mustBeAuthenticated: Boolean
    ) : Pair<EvaluatedIndividual<RestIndividual>, Endpoint>?{

        val existingPuts  = findIndividuals(
            individuals,
            HttpVerb.PUT,
            resourcePath,
            201, //if PUT, must be 201
            statusGroup = null,
            authenticated = mustBeAuthenticated
        )

        if(existingPuts.isNotEmpty()){
            return Pair(
                existingPuts.sortedBy { it.individual.size() }[0],
                Endpoint(HttpVerb.PUT, resourcePath)
            )
        }

        if(resourcePath.isRoot()){
            return null
        }

        val parent = resourcePath.parentPath()

        val existingPosts = findIndividuals(
            individuals,
            HttpVerb.POST, // it is not uncommon to give a 200 even if creating new resource
            parent,
            statusGroup = StatusGroup.G_2xx,
            authenticated = mustBeAuthenticated
        )

        if(existingPosts.isNotEmpty()){
            return Pair(
                existingPosts.sortedBy { it.individual.size() }[0],
                Endpoint(HttpVerb.POST, parent)
            )
        }

        //found nothing
        return null
    }


    /**
     * @return a negative value if no action with the given properties is found in the individual.
     */
    @Deprecated(message = "Use findIndexOfAction")
    fun getIndexOfAction(individual: EvaluatedIndividual<RestIndividual>,
                         verb: HttpVerb,
                         path: RestPath,
                         statusCode: Int
    ) : Int {
        return findIndexOfAction(individual, verb, path, statusCode)
    }

    /**
     * @return a negative value if no action with the given properties is found in the individual.
     */
    fun findIndexOfAction(
        individual: EvaluatedIndividual<RestIndividual>,
        verb: HttpVerb? = null,
        path: RestPath? = null,
        status: Int? = null,
        statusGroup: StatusGroup? = null,
        statusCodes: Collection<Int>? = null,
        authenticated: Boolean? = null,
        authenticatedWith: String? = null
    ): Int {

        if(status != null && statusGroup!= null){
            throw IllegalArgumentException("Shouldn't specify both status and status group")
        }

        individual.evaluatedMainActions().forEachIndexed { index, a ->
                if(checkIfActionSatisfiesConditions(a, verb, path, status, statusGroup, statusCodes, authenticated, authenticatedWith)){
                    return index
                }
        }
        return -1
    }
}
