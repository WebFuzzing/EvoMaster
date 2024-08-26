package org.evomaster.core.problem.rest

import org.apache.http.HttpStatus
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.ActionResult

object RestSecurityOracle {


    /**
     * Check if the last 3 actions represent the following scenario:
     * - authenticated user A creates a resource X (status 2xx). could be SQL
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     *
     * if so, a new "fault" target is added to the fitness function
     *
     * @return true if test detect such fault
     */
    fun hasForbiddenDelete(individual: RestIndividual,
                           actionResults: List<ActionResult>
    ) : Boolean{

        if(individual.sampleType != SampleType.SECURITY){
            throw IllegalArgumentException("We verify security properties only on tests constructed to check them")
        }

        // get actions in the individual
        val actions = individual.seeMainExecutableActions()
        val numberOfActions = actions.size

        // make sure that there are at least 3 actions
        // Note: it does not matter of 3-last action creating the resource.
        // it mattered when creating the test, but not here when evaluating the oracle.
        // by all means, it could had been done with SQL insertions
        if (numberOfActions < 2) {
            return false
        }


        // last 2 actions
        val lastAction = actions[numberOfActions - 1]
        val secondLastAction = actions[numberOfActions - 2]
        //val thirdLastAction = actions[numberOfActions - 3]

        val restCallResults = actionResults.filterIsInstance<RestCallResult>()

        // last 3 results
        val lastResult = restCallResults.find { it.sourceLocalId == lastAction.getLocalId() }
                ?.getStatusCode() ?: return false
        val secondLastResult = restCallResults.find { it.sourceLocalId == secondLastAction.getLocalId() }
                ?.getStatusCode() ?: return false
//        val thirdLastResult = restCallResults.find { it.sourceLocalId == thirdLastAction.getLocalId() }
//                ?.getStatusCode() ?: return false


        // first check that they all refer to the same endpoint
        val conditionForEndpointEquivalence =
                lastAction.resolvedOnlyPath() == secondLastAction.resolvedOnlyPath()
                        //&& secondLastAction.resolvedOnlyPath() == thirdLastAction.resolvedOnlyPath()

        if (!conditionForEndpointEquivalence) {
            return false
        }
        // meaning the first put/post (not necessarily needed),
        // the second delete and the last put/patch, all on same resource

        // if the authentication of last and the authentication of second last are not the same
        // return null
        if (lastAction.auth.isDifferentFrom(secondLastAction.auth)) {
            return false
        }

        // if the authentication of third last and the authentication of first/second last are the same,
        // ie, they all use same auth, then return null
//        if (!thirdLastAction.auth.isDifferentFrom(secondLastAction.auth)) {
//            return false
//        }

        var firstCondition = false
        var secondCondition = false
//        var thirdCondition = false

        // last action should be a PUT/PATCH action with wrong success statusCode instead of forbidden as DELETE
        if (
            (lastAction.verb == HttpVerb.PUT || lastAction.verb == HttpVerb.PATCH)
            && StatusGroup.G_2xx.isInGroup(lastResult)) {
            firstCondition = true
        }

        // forbidden DELETE for auth
        if (secondLastAction.verb == HttpVerb.DELETE && secondLastResult == HttpStatus.SC_FORBIDDEN) {
            secondCondition = true
        }

        //creation operation with different auth
//        if (
//            (thirdLastAction.verb == HttpVerb.PUT || thirdLastAction.verb == HttpVerb.POST)
//            && StatusGroup.G_2xx.isInGroup(thirdLastResult)
//            ) {
//            thirdCondition = true
//        }

        if ( !(firstCondition && secondCondition
                //    && thirdCondition
                ) ) {
            return false
        }

        return true
    }

}