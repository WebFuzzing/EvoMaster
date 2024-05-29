package org.evomaster.core.problem.rest

import org.apache.http.HttpStatus
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult

object RestSecurityOracle {


    /**
     * Check if the last 3 actions represent the following scenario:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     *
     * if so, a new "fault" target is added to the fitness function
     *
     * @return the name chosen for the found fault, if any. null otherwise
     */
    fun handleForbiddenDelete(individual: RestIndividual,
                              actionResults: List<ActionResult>,
                              fv: FitnessValue
    ) : String?{

        if(individual.sampleType != SampleType.SECURITY){
            throw IllegalArgumentException("We verify security properties only on tests constructed to check them")
        }

        //TODO

        // get actions in the individual
        val actions = individual.seeMainExecutableActions()
        val numberOfActions = actions.size

        // make sure that there are at least 3 actions
        if (numberOfActions < 3) {
            return null
        }
        else {

            // last 3 actions
            val lastAction = actions[numberOfActions - 1]
            val secondLastAction = actions[numberOfActions - 2]
            val thirdLastAction = actions[numberOfActions - 3]

            // last 3 results
            val lastResult = Integer.parseInt(actionResults[numberOfActions - 1].getResultValue("STATUS_CODE"))
            val secondLastResult = Integer.parseInt(actionResults[numberOfActions - 2].getResultValue("STATUS_CODE"))
            val thirdLastResult = Integer.parseInt(actionResults[numberOfActions - 3].getResultValue("STATUS_CODE"))

            // first check that they all refer to the same endpoint //TODO
            val conditionForEndpointEquivalence =
                lastAction.resolvedPath().split("?")[0] == secondLastAction.resolvedPath().split("?")[0] &&
                        secondLastAction.resolvedPath().split("?")[0] == thirdLastAction.resolvedPath().split("?")[0]

            if (!conditionForEndpointEquivalence) {
                return null
            }
            else {

                // meaning the first put, the second delete and the last put.
                // also check that authentication information TODO

                // if the authentication of last and the authentication of second last are not the same
                // return null
                if (!AuthUtils.checkIdenticalAuthentication(lastAction.auth, secondLastAction.auth)) {
                    return null
                }

                // if the authentication of third last and the authentication of first last are the same
                // return null
                if (AuthUtils.checkIdenticalAuthentication(thirdLastAction.auth, secondLastAction.auth)) {
                    return null
                }

                // last action should be a PUT action with statusCode
                // lastAction.verb == HttpVerb.PUT && lastResult.
                var firstCondition = false
                var secondCondition = false
                var thirdCondition = false

                if (lastAction.verb == HttpVerb.PUT && StatusGroup.G_2xx.isInGroup(lastResult)) {
                    firstCondition = true
                }

                if (secondLastAction.verb == HttpVerb.DELETE && secondLastResult == HttpStatus.SC_FORBIDDEN) {
                    secondCondition = true
                }

                if (thirdLastAction.verb == HttpVerb.PUT && StatusGroup.G_2xx.isInGroup(thirdLastResult)) {
                    thirdCondition = true
                }

                if ( !(firstCondition && secondCondition && thirdCondition) ) {
                    return null
                }
            }

        }

        return "TestingTargetAccessControl"
    }

}