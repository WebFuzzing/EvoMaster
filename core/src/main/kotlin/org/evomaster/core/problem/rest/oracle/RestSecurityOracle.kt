package org.evomaster.core.problem.rest.oracle

import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import org.apache.http.HttpStatus
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.utils.StackTraceUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class RestSecurityOracle {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RestSecurityOracle::class.java)

        /**
         * Simple SQLi payloads. Used to check for SQL Injection vulnerability.
         * The payloads are designed to introduce delays in the database response,
         * which can be detected by measuring the response time of the application.
         */
        val SQLI_PAYLOADS = listOf(
            // Simple sleep-based payloads for MySQL
            "' OR SLEEP(%.2f)-- -",
            "\" OR SLEEP(%.2f)-- -",
            "' OR SLEEP(%.2f)=0-- -",
            "\" OR SLEEP(%.2f)=0-- -",
            // Integer-based delays
            "' OR SLEEP(%.0f)-- -",
            "\" OR SLEEP(%.0f)-- -",
            "' OR SLEEP(%.0f)=0-- -",
            "\" OR SLEEP(%.0f)=0-- -",
            // Simple sleep-based payloads for PostgreSQL
            "' OR select pg_sleep(%.2f)-- -",
            "\" OR select pg_sleep(%.2f)-- -",
            "' OR (select pg_sleep(%.2f)) IS NULL-- -",
            "\' OR (select pg_sleep(%.2f)) IS NULL-- -",
            // Integer-based delays
            "' OR select pg_sleep(%.0f)-- -",
            "\" OR select pg_sleep(%.0f)-- -",
            "' OR (select pg_sleep(%.0f)) IS NULL-- -",
            "\' OR (select pg_sleep(%.0f)) IS NULL-- -",
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

    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject
    private lateinit var ssrfAnalyser: SSRFAnalyser

    @Inject
    protected lateinit var callGraphService: CallGraphService

    /**
     * Evaluate all the different security oracles, based on what enabled in configurations,
     * and update the fitness value if any fault is found.
     * Each security category has its own unique fault identifier.
     */
    fun analyzeSecurityProperties(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ){
        handleForbiddenOperation(HttpVerb.DELETE, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleForbiddenOperation(HttpVerb.PUT, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleForbiddenOperation(HttpVerb.PATCH, DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION, individual, actionResults, fv)
        handleExistenceLeakage(individual,actionResults,fv)
        handleNotRecognizedAuthenticated(individual, actionResults, fv)
        handleForgottenAuthentication(individual, actionResults, fv)
        handleStackTraceCheck(individual, actionResults, fv)
        handleSQLiCheck(individual, actionResults, fv)
        handleXSSCheck(individual, actionResults, fv)
        handleAnonymousWriteCheck(individual, actionResults, fv)
        handleHiddenAccessible(individual, actionResults, fv)
        handleSsrfFaults(individual, actionResults, fv)
    }

    private fun handleSsrfFaults(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (!config.isEnabledFaultCategory(DefinedFaultCategory.SSRF)) {
            return
        }

        individual.seeMainExecutableActions().forEach {
            val ar = (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult?)
            if (ar != null) {
                if (ar.getResultValue(HttpWsCallResult.VULNERABLE_SSRF).toBoolean()) {
                    val scenarioId = idMapper.handleLocalTarget(
                        idMapper.getFaultDescriptiveId(DefinedFaultCategory.SSRF, it.getName())
                    )
                    fv.updateTarget(scenarioId, 1.0, it.positionAmongMainActions())

                    val paramName = ssrfAnalyser.getVulnerableParameterName(it)
                    ar.addFault(DetectedFault(DefinedFaultCategory.SSRF, it.getName(), paramName))
                }
            }
        }
    }

    private fun handleNotRecognizedAuthenticated(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (!config.isEnabledFaultCategory(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED)) {
            return
        }

        if(actionResults.any { it.stopping }){
            return
        }

        val notRecognized = individual.seeMainExecutableActions()
            .filter {
                val ar = actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult?
                if(ar == null){
                    log.warn("Missing action result with id: ${it.getLocalId()}}")
                    false
                } else {
                    it.auth !is NoAuth && ar.getStatusCode() == 401
                }
            }
            .filter {
                hasNotRecognizedAuthenticated(it, individual, actionResults)
            }

        if(notRecognized.isEmpty()){
            return
        }

        notRecognized.forEach {
            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, it.getName())
            )
            fv.updateTarget(scenarioId, 1.0, it.positionAmongMainActions())
            val r = actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult
            r.addFault(DetectedFault(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, it.getName(), null))
        }
    }

    private fun handleExistenceLeakage(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (!config.isEnabledFaultCategory(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE)) {
            return
        }

        val getPaths = individual.seeMainExecutableActions()
            .filter { it.verb == HttpVerb.GET }
            .map { it.path }
            .toSet()

        val faultyPaths = getPaths.filter {
            hasExistenceLeakage(it, individual, actionResults)
        }
        if(faultyPaths.isEmpty()){
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult

            if(a.verb == HttpVerb.GET && faultyPaths.contains(a.path) && r.getStatusCode() == 404){
                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE, a.getName(), null))
            }
        }
    }

    private fun handleSQLiCheck(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (!config.isEnabledFaultCategory(DefinedFaultCategory.SQL_INJECTION)) {
            return
        }

        val foundPair = findSQLiPayloadPair(individual)

        if(foundPair == null){
            //no pair found, cannot do baseline comparison
            return
        }

        val (actionWithoutPayload, actionWithPayload) = foundPair
        val baselineResult = actionResults.find { it.sourceLocalId == actionWithoutPayload.getLocalId() } as? RestCallResult
            ?: return
        val baselineTime = baselineResult.getResponseTimeMs() ?: return

        val injectedResult = actionResults.find { it.sourceLocalId == actionWithPayload.getLocalId() } as? RestCallResult
            ?: return
        val injectedTime = injectedResult.getResponseTimeMs()


        val K = config.sqliBaselineMaxResponseTimeMs        // K: maximum allowed baseline response time
        val N = config.sqliInjectedSleepDurationMs          // N: expected delay introduced by the injected sleep payload

        // Baseline must be fast enough (baseline < K)
        val baselineIsFast = baselineTime < K

        // Response after injection must be slow enough (response > N)
        var responseIsSlowEnough: Boolean

        if (injectedTime != null) {
            responseIsSlowEnough = injectedTime > N
        } else if (injectedResult.getTimedout()){
            // if the injected request timed out, we can consider it vulnerable
            responseIsSlowEnough = true
        } else {
            return
        }

        // If baseline is fast AND the response after payload is slow enough,
        // then we consider this a potential time-based SQL injection vulnerability.
        // Otherwise, skip this result.
        if (!(baselineIsFast && responseIsSlowEnough)) {
            return
        }

        // Find the index of the action with payload to report it correctly
        val index = individual.seeMainExecutableActions().indexOf(actionWithPayload)
        if (index < 0) {
            log.warn("Failed to find index of action with SQLi payload")
            return
        }

        val scenarioId = idMapper.handleLocalTarget(
            idMapper.getFaultDescriptiveId(DefinedFaultCategory.SQL_INJECTION, actionWithPayload.getName())
        )
        fv.updateTarget(scenarioId, 1.0, index)
        injectedResult.addFault(DetectedFault(DefinedFaultCategory.SQL_INJECTION, actionWithPayload.getName(), null))
        injectedResult.setVulnerableForSQLI(true)
    }

    /**
     * Finds a pair of actions in the individual that have the same path and verb,
     * where one contains a SQLi payload and the other does not.
     *
     * This is useful for comparing baseline response times (without payload) against
     * response times with SQLi payload to detect time-based SQL injection vulnerabilities.
     *
     * @param individual The test individual to search
     * @return A pair of (actionWithoutPayload, actionWithPayload), or null if no such pair exists
     */
    private fun findSQLiPayloadPair(
        individual: RestIndividual
    ): Pair<RestCallAction, RestCallAction>? {

        val actions = individual.seeMainExecutableActions()
            .filterIsInstance<RestCallAction>()

        // Group actions by path and verb
        val actionsByPathAndVerb = actions
            .groupBy { it.path.toString() to it.verb }

        // Find a pair where one has SQLi payload and one doesn't
        for ((pathVerb, actionsForPath) in actionsByPathAndVerb) {
            if (actionsForPath.size < 2) continue

            val withPayload = actionsForPath.filter {
                hasSQLiPayload(it, config.sqliInjectedSleepDurationMs/1000.0)
            }
            val withoutPayload = actionsForPath.filter {
                !hasSQLiPayload(it, config.sqliInjectedSleepDurationMs/1000.0)
            }

            if (withPayload.isNotEmpty() && withoutPayload.isNotEmpty()) {
                return Pair(withoutPayload.first(), withPayload.first())
            }
        }

        return null
    }

    private fun handleStackTraceCheck(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if (!config.isEnabledFaultCategory(ExperimentalFaultCategory.LEAKED_STACK_TRACES)) {
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult?
            //this can happen if an action timeout, or is stopped
                ?: continue

            if(r.getStatusCode() == 500 && r.getBody() != null && StackTraceUtils.looksLikeStackTrace(r.getBody()!!)){
                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.LEAKED_STACK_TRACES, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(ExperimentalFaultCategory.LEAKED_STACK_TRACES, a.getName(), null))
            }
        }
    }

    private fun handleHiddenAccessible(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ){
        if(!config.isEnabledFaultCategory(ExperimentalFaultCategory.HIDDEN_ACCESSIBLE_ENDPOINT)){
            return
        }

        for(index in 0 until individual.seeMainExecutableActions().lastIndex) {
            val a = individual.seeMainExecutableActions()[index]
            if(a.verb != HttpVerb.OPTIONS){
                continue
            }
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult?
            //this can happen if an action timeout, or is stopped
                ?: break
            val allowedVerbs = r.getAllowedVerbs()
                ?: continue

            val path = a.path
            val hidden = allowedVerbs.filter { it != HttpVerb.OPTIONS && !callGraphService.isDeclared(it, path) }

            val target = individual.seeMainExecutableActions()[index+1]
            if(target.path != path || !hidden.contains(target.verb)){
                continue
            }

            val data = actionResults.find { it.sourceLocalId == target.getLocalId() } as RestCallResult?
                ?: break

            val status = data.getStatusCode()
                ?: continue

            if(status !in setOf(405,501,403)){
                // we also consider 403, in case API just give it by default for security reasons

                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.HIDDEN_ACCESSIBLE_ENDPOINT, target.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index+1)
                data.addFault(DetectedFault(ExperimentalFaultCategory.HIDDEN_ACCESSIBLE_ENDPOINT, target.getName(), null))
            }
        }
    }


    private fun handleXSSCheck(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if(!config.isEnabledFaultCategory(DefinedFaultCategory.XSS)){
            return
        }

        // Check if this individual has XSS vulnerability
        if(!hasXSS(individual, actionResults)){
            return
        }

        // Find the action(s) where XSS payload appears in the response
        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as? RestCallResult
                ?: continue

            if(!StatusGroup.G_2xx.isInGroup(r.getStatusCode())){
                continue
            }

            val responseBody = r.getBody() ?: continue

            // Check if any XSS payload is present in this response
            for(payload in XSS_PAYLOADS){
                if(responseBody.contains(payload, ignoreCase = false)){
                    val scenarioId = idMapper.handleLocalTarget(
                        idMapper.getFaultDescriptiveId(DefinedFaultCategory.XSS, a.getName())
                    )
                    fv.updateTarget(scenarioId, 1.0, index)
                    r.addFault(DetectedFault(DefinedFaultCategory.XSS, a.getName(), null))
                    break // Only add one fault per action
                }
            }
        }
    }

    private fun handleAnonymousWriteCheck(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {
        if(!config.isEnabledFaultCategory(ExperimentalFaultCategory.ANONYMOUS_MODIFICATIONS)){
            return
        }

        // Get all write operation paths (PUT, PATCH, DELETE)
        val writePaths = individual.seeMainExecutableActions()
            .filter { it.verb == HttpVerb.PUT || it.verb == HttpVerb.PATCH || it.verb == HttpVerb.DELETE }
            .map { it.path }
            .toSet()

        val faultyPaths = writePaths.filter { hasAnonymousWrite(it, individual, actionResults) }

        if(faultyPaths.isEmpty()){
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult

            if((a.verb == HttpVerb.PUT || a.verb == HttpVerb.PATCH || a.verb == HttpVerb.DELETE)
                && faultyPaths.contains(a.path)
                && a.auth is NoAuth
                && StatusGroup.G_2xx.isInGroup(r.getStatusCode())){

                // For PUT, check if it's 201 (resource creation - might be OK)
                if(a.verb == HttpVerb.PUT && r.getStatusCode() == 201){
                    continue
                }

                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.ANONYMOUS_MODIFICATIONS, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(ExperimentalFaultCategory.ANONYMOUS_MODIFICATIONS, a.getName(), null))
            }
        }
    }

    private fun handleForgottenAuthentication(
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {

        if (!config.isEnabledFaultCategory(ExperimentalFaultCategory.IGNORE_ANONYMOUS)) {
            return
        }

        val endpoints = individual.seeMainExecutableActions()
            .map { it.getName() }
            .toSet()

        val faultyEndpoints = endpoints.filter { hasForgottenAuthentication(it, individual, actionResults)  }

        if(faultyEndpoints.isEmpty()){
            return
        }

        for(index in individual.seeMainExecutableActions().indices){
            val a = individual.seeMainExecutableActions()[index]
            val r = actionResults.find { it.sourceLocalId == a.getLocalId() } as RestCallResult

            if(a.auth is NoAuth && faultyEndpoints.contains(a.getName()) &&  StatusGroup.G_2xx.isInGroup(r.getStatusCode())){
                val scenarioId = idMapper.handleLocalTarget(
                    idMapper.getFaultDescriptiveId(ExperimentalFaultCategory.IGNORE_ANONYMOUS, a.getName())
                )
                fv.updateTarget(scenarioId, 1.0, index)
                r.addFault(DetectedFault(ExperimentalFaultCategory.IGNORE_ANONYMOUS, a.getName(), null))
            }
        }
    }

    private fun handleForbiddenOperation(
        verb: HttpVerb,
        faultCategory: FaultCategory,
        individual: RestIndividual,
        actionResults: List<ActionResult>,
        fv: FitnessValue
    ) {

        if (!config.isEnabledFaultCategory(DefinedFaultCategory.SECURITY_WRONG_AUTHORIZATION)) {
            return
        }

        if (hasForbiddenOperation(verb, individual, actionResults)) {
            val actionIndex = individual.size() - 1
            val action = individual.seeMainExecutableActions()[actionIndex]
            val result = actionResults
                .filterIsInstance<RestCallResult>()
                .find { it.sourceLocalId == action.getLocalId() }
                ?: return

            val scenarioId = idMapper.handleLocalTarget(
                idMapper.getFaultDescriptiveId(faultCategory, action.getName())
            )
            fv.updateTarget(scenarioId, 1.0, actionIndex)
            result.addFault(DetectedFault(faultCategory, action.getName(), null))
        }
    }


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

    /**
     * Check for anonymous write vulnerability - write operations without authentication.
     *
     * Flags as vulnerability:
     * - PUT returning 2xx (except 201) without authentication - updating existing resources
     * - PATCH returning 2xx without authentication - partial updates
     * - DELETE returning 2xx without authentication - deleting resources
     *
     * Note: PUT returning 201 (creating new resource) might be acceptable for public endpoints
     * like adding entries to a public forum.
     *
     * @param path the REST path to check
     * @param individual the test individual (must be of SampleType.SECURITY)
     * @param actionResults the results of executing the actions
     * @return true if anonymous write vulnerability is detected
     */
    fun hasAnonymousWrite(
        path: RestPath,
        individual: RestIndividual,
        actionResults: List<ActionResult>
    ): Boolean{

        verifySampleType(individual)

        // Get all write operations (PUT, PATCH, DELETE) on the given path
        val actions = individual.seeMainExecutableActions().filter {
            (it.verb == HttpVerb.PUT || it.verb == HttpVerb.PATCH || it.verb == HttpVerb.DELETE)
            && it.path.isEquivalent(path)
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

        // Check for write operations without authentication that return 2xx
        val anonymousWriteActions = actionsWithResults.filter {
            it.auth is NoAuth &&
            StatusGroup.G_2xx.isInGroup((actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult)
                .getStatusCode())
        }

        if(anonymousWriteActions.isEmpty()){
            return false
        }

        // For PUT, check if it's 201 (resource creation - might be OK) or other 2xx (update - vulnerability)
        // For PATCH/DELETE: any 2xx is a vulnerability
        return anonymousWriteActions.any { action ->
            val statusCode = (actionResults.find { r -> r.sourceLocalId == action.getLocalId() } as RestCallResult)
                .getStatusCode()

            when(action.verb){
                HttpVerb.PUT -> statusCode != 201  // Only flag if NOT creating new resource
                HttpVerb.PATCH, HttpVerb.DELETE -> true  // Any 2xx is a vulnerability
                else -> false
            }
        }
    }

    fun hasExistenceLeakage(
        path: RestPath,
        individual: RestIndividual,
        actionResults: List<ActionResult>,
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

        if(a403.isEmpty() || a404.isEmpty()){
            //no discrepancy of status on same path. so no leakage
            return false
        }

        /*
            by itself, the fact that there is 404 does not imply a leakage, as the user
            "might" own parent resources.
            we need to check for that.
         */

        val topGet = callGraphService.findStrictTopGETResourceAncestor(path)
            //if null, then for sure the user with 404 does not own a parent resource
            ?: return true

        val verifiers = individual.seeMainExecutableActions()
            .filter { it.verb == HttpVerb.GET && it.path == topGet.path }
            .filter { actionResults.find { r -> r.sourceLocalId == it.getLocalId() } != null }

        if(verifiers.isEmpty()){
            //a top GET exists in schema, but was not called in the test.
            //as such, we cannot be sure if bug is found, as, even if 403-404 on same path,
            //it could well be that the 404 was legit
            return false
        }

        for(notfound in a404){

            //FIXME i don't think it is correct, as ignoring dynamic info?
            //TODO need tests for it
            val matching = verifiers.filter {
                it.isResolvedParentPath(notfound)
                        && ! notfound.auth.isDifferentFrom(it.auth)
            }

            if(matching.isEmpty()){
                continue
            }

            val codes = matching.map { (actionResults.find { r -> r.sourceLocalId == it.getLocalId() } as RestCallResult).getStatusCode() }

            if(codes.any{ StatusGroup.G_2xx.isInGroup(it)}) {
                //a 2xx can be done on parent resource
                continue
            }

            if(codes.any{ it == 403 || it == 404 }) {
                //there is at least one call on ancestor resource with same auth, but none was positive 2xx
                return true // and there is at least one discrepancy 403-404 on same endpoint
            }
            //other codes like 400 or 500 are ignored here, eg, due to input validation
        }

        return false
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

    fun hasSQLiPayload(action: RestCallAction, duration: Double): Boolean {
        val allValues = action.seeTopGenes()
            .map { it.getValueAsRawString() }
            .joinToString(" ")

        return SQLI_PAYLOADS.any { payload ->
            allValues.contains(String.format(payload, duration), ignoreCase = true)
        }
    }



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
