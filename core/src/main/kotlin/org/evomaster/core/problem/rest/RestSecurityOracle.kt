package org.evomaster.core.problem.rest

import org.apache.http.HttpStatus
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.ActionResult

object RestSecurityOracle {


    /**
     * For example verb target DELETE,
     * check if the last 2 actions represent the following scenario:
     * - authenticated user A gets 403 on DELETE X
     * - authenticated user A gets 200 on PUT/PATCH on X
     *
     *  If so, it is a fault.
     *
     * Note, here in the oracle check, it does not matter how the resource
     * was created, eg with a POST/PUT using different auth, or directly
     * with database insertion
     *
     * @return true if test detect such fault
     */
    fun hasForbiddenOperation(
        verb: HttpVerb,
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ) : Boolean{

        if(individual.sampleType != SampleType.SECURITY){
            throw IllegalArgumentException("We verify security properties only on tests constructed to check them")
        }

        // get actions in the individual
        val actions = individual.seeMainExecutableActions()
        val numberOfActions = actions.size

        // make sure that there are at least 2 actions
        // Note: it does not matter of 3rd-last action creating the resource.
        // it mattered when creating the test, but not here when evaluating the oracle.
        // by all means, it could had been done with SQL insertions
        if (numberOfActions < 2) {
            return false
        }

        // last 2 actions
        val lastAction = actions[numberOfActions - 1]
        val secondLastAction = actions[numberOfActions - 2]

        val restCallResults = actionResults.filterIsInstance<RestCallResult>()

        // last 2 results
        val lastResult = restCallResults.find { it.sourceLocalId == lastAction.getLocalId() }
                ?.getStatusCode() ?: return false
        val secondLastResult = restCallResults.find { it.sourceLocalId == secondLastAction.getLocalId() }
                ?.getStatusCode() ?: return false

        // first check that they all refer to the same endpoint
        val conditionForEndpointEquivalence =
                PostCreateResourceUtils.resolveToSamePath(lastAction, secondLastAction)

        if (!conditionForEndpointEquivalence) {
            return false
        }
        // meaning the 3rd-last put/post (not necessarily needed),
        // the 2nd-last delete and the last put/patch, all on same resource

        // if the authentication of last and the authentication of second last are not the same
        // return null
        if (lastAction.auth.isDifferentFrom(secondLastAction.auth)) {
            return false
        }

        var firstCondition = false
        var secondCondition = false

        // last action should be a PUT/PATCH action with wrong success statusCode instead of forbidden as DELETE
        val others = HttpVerb.otherWriteOperationsOnSameResourcePath(verb)
        if (others.contains(lastAction.verb) && StatusGroup.G_2xx.isInGroup(lastResult)) {
            firstCondition = true
        }

        // forbidden DELETE for auth
        if (secondLastAction.verb == verb && secondLastResult == HttpStatus.SC_FORBIDDEN) {
            secondCondition = true
        }


        if ( !(firstCondition && secondCondition) ) {
            return false
        }

        return true
    }

}