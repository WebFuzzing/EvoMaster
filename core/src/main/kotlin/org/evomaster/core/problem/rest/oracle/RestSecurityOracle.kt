package org.evomaster.core.problem.rest.oracle

import org.apache.http.HttpStatus
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.search.action.ActionResult

object RestSecurityOracle {

    private fun verifySampleType(individual: RestIndividual){
        if(individual.sampleType != SampleType.SECURITY){
            throw IllegalArgumentException("We verify security properties only on tests constructed to check them")
        }
    }

    fun hasNotRecognizedAuthenticated(
        action: RestCallAction,
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean{
        verifySampleType(individual)

        if(action.auth is NoAuth){
            return false
        }
        if((actionResults.find { it.sourceLocalId == action.getLocalId() } as RestCallResult).getStatusCode() != 401){
            return false
        }

        //got a 2xx on other endpoint
        val wasOk = individual.seeMainExecutableActions()
            .filter { !it.auth.isDifferentFrom(action.auth)
                    && StatusGroup.G_2xx.isInGroup(
                (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult).getStatusCode())}
            .map { it.getName() }
        if(wasOk.isEmpty()){
            return false
        }

        /*
            to check if endpoint needs auth, need either a 401 or 403, regardless of user.
            it can be the same user, eg, accessing resource created by another user
         */
        return individual.seeMainExecutableActions().any {
                    // checking endpoint in which target user got a 2xx
                    wasOk.contains(it.getName())
                    //but here this other user got a 401 or 403, so the endpoint requires auth
                    && listOf(401,403).contains((actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                        .getStatusCode())
        }
    }

    fun hasForgottenAuthentication(
        endpoint: String,
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean{

        verifySampleType(individual)

        val actions = individual.seeMainExecutableActions().filter {
            it.getName() == endpoint
        }

        val actionsWithResults = actions.filter {
            //can be null if sequence was stopped
            actionResults.find { r -> r.sourceLocalId == it.getLocalId() } != null
        }

        if(actions.size != actionsWithResults.size){
            assert(actionResults.any { it.stopping }) {
                "Not all actions have results, but sequence was not stopped"
            }
        }

        /*
         Check if there is any protected resource (i.e., one that returns 403 or 401 when accessed without proper authorization),
         but the same resource is also accessible without any authentication.
         */

        val a403 = actionsWithResults.filter {
            (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                .getStatusCode() == 403
        }

        val a401 = actionsWithResults.filter {
            (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                .getStatusCode() == 401
        }

        val a2xxWithoutAuth = actionsWithResults.filter {
             StatusGroup.G_2xx.isInGroup((actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                 .getStatusCode())
        }.filter {
            // check if the action is not authenticated
            it.auth is NoAuth
        }

        return (a403.isNotEmpty() || a401.isNotEmpty()) && a2xxWithoutAuth.isNotEmpty()
    }

    fun hasExistenceLeakage(
        path: RestPath,
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean{

        verifySampleType(individual)

        val actions = individual.seeMainExecutableActions()
            .filter {
                it.verb == HttpVerb.GET && it.path == path
            }

        val actionsWithResults = actions.filter {
            //can be null if sequence was stopped
            actionResults.find { r -> r.sourceLocalId == it.getLocalId() } != null
        }

        if(actions.size != actionsWithResults.size){
            assert(actionResults.any { it.stopping }) {
                "Not all actions have results, but sequence was not stopped"
            }
        }

        val a403 = actionsWithResults.filter {
                        (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                    .getStatusCode() == 403
            }
        val a404 = actionsWithResults.filter {
                        (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                    .getStatusCode() == 404
            }

        return a403.isNotEmpty() && a404.isNotEmpty()
    }

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

        verifySampleType(individual)

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
            CreateResourceUtils.doesResolveToSamePath(lastAction, secondLastAction)

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

    // Simple SQLi payloads
    val SQLI_PAYLOADS = listOf(
        "' OR (WITH RECURSIVE r(i) AS (SELECT 1 UNION ALL SELECT i+1 FROM r WHERE i < 10000000) SELECT COUNT(*) FROM r)>0--",
        "' OR SLEEP(5)-- -",
        "\" OR SLEEP(5)-- -",
        "' union select sleep(5)-- -",
        "\" union select sleep(5)-- -",
        "' OR pg_sleep(5)-- -",
        "\" OR pg_sleep(5)-- -",
        "' union select pg_sleep(5)-- -",
        "\" union select pg_sleep(5)-- -",
        // for h2 database
//        "' OR (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B, INFORMATION_SCHEMA.COLUMNS C)>0--",
//        "' OR (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS A, INFORMATION_SCHEMA.COLUMNS B)>0--",
//        it takes extremely long.
//        "' OR (SELECT SUM(a.ORDINAL_POSITION*b.ORDINAL_POSITION*c.ORDINAL_POSITION) FROM INFORMATION_SCHEMA.COLUMNS a, INFORMATION_SCHEMA.COLUMNS b, INFORMATION_SCHEMA.COLUMNS c)>1 --",
    )

    
    // Simple XSS payloads inspired by big-list-of-naughty-strings
    // https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
    val XSS_PAYLOADS = listOf(
        "<img src=x onerror=alert('XSS')>",
        "<svg onload=alert('XSS')>",
        "<details open ontoggle=alert('XSS')>",
        "<script>alert('XSS')</script>",
        "<iframe src='javascript:alert(\"XSS\")'></iframe>"
    )


    /**
     * Check for XSS (Cross-Site Scripting) vulnerability.
     *
     * This checks if an XSS payload injected into a POST/PUT/PATCH request is reflected
     * in the response (reflected XSS) or appears in a subsequent GET request (stored XSS).
     *
     * @param individual the test individual (must be of SampleType.SECURITY)
     * @param actionResults the results of executing the actions
     * @return true if XSS vulnerability is detected
     */
    fun hasXSS(
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean {

        verifySampleType(individual)

        val actions = individual.seeMainExecutableActions()

        if(actions.isEmpty()){
            return false
        }

        // Check each action that might contain XSS payload
        for(action in actions){
            if(action.verb != HttpVerb.POST && action.verb != HttpVerb.PUT && action.verb != HttpVerb.PATCH && action.verb != HttpVerb.GET){
                continue
            }

            val result = actionResults.find { r -> r.sourceLocalId == action.getLocalId() } as? RestCallResult
                ?: continue

            // Only check if request was successful
            if(!StatusGroup.G_2xx.isInGroup(result.getStatusCode())){
                continue
            }

            val responseBody = result.getBody() ?: continue

            // Check if any XSS payload is present in the response
            for(payload in XSS_PAYLOADS){
                if(responseBody.contains(payload, ignoreCase = false)){
                    // Found XSS payload in response
                    return true
                }
            }
        }

        return false
    }

}
