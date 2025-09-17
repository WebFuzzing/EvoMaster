package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import javax.annotation.PostConstruct

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.AuthSettings
import org.evomaster.core.problem.enterprise.auth.NoAuth
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler

import org.evomaster.core.search.*
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Service class used to do security testing after the search phase.
 *
 * This class can add new test cases to the archive that, by construction, do reveal a security fault.
 * But, the actual check if a test indeed finds a fault is in [RestSecurityOracle]
 * called in the fitness function, and not directly here.
 */
class SecurityRest {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SecurityRest::class.java)
    }

    /**
     * Archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: AbstractRestSampler

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var fitness: FitnessFunction<RestIndividual>

    @Inject
    private lateinit var idMapper: IdMapper

    @Inject
    private lateinit var builder: RestIndividualBuilder

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions: List<RestCallAction>


    /**
     * Individuals in the solution.
     * Derived from archive.
     */
    private lateinit var individualsInSolution: List<EvaluatedIndividual<RestIndividual>>

    private lateinit var authSettings: AuthSettings

    /**
     * Function called after init. This function initializes REST sampler definitions
     * and authentication settings.
     */
    @PostConstruct
    private fun postInit() {

        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>

        authSettings = sampler.authentications
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase(): Solution<RestIndividual> {

        // extract individuals from the archive
        individualsInSolution = this.archive.extractSolution().individuals

        expandWithForbidden()
        //recompute due to possible new tests we might need
        individualsInSolution = this.archive.extractSolution().individuals


        // we can see what is available from the schema, and then check if already existing a test for it in archive
        // newly generated tests will be added back to archive
        addForAccessControl()

        //TODO possible other kinds of security tests here

        // just return the archive for solutions including the security tests.
        return archive.extractSolution()
    }


    /**
     * During the search, we do not explicitly try different users in the same test case.
     * as such, getting tests with 403 might be tricky.
     * but having such tests might be necessary for some types of oracles we designed
     */
    private fun expandWithForbidden() {

        actionDefinitions.forEach { op ->

            val forbidden = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                status = 403
            )
            if(forbidden.isNotEmpty()){
                //we already have it, so nothing to do
                return@forEach
            }

            val unauthorized = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                status = 401
            )
            if(unauthorized.isEmpty()){
                //there is no 401, so does not seem auth is applied to this endpoint.
                //note: getting 401 during search should be simple, as we do send requests without
                // auth, given a certain probability
                return@forEach
            }

            val candidates = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                statusGroup = StatusGroup.G_2xx,
                authenticated = true
            )

            if (candidates.isNotEmpty()){
                handleCandidatesFor403(op.verb, op.path, candidates)
            }
        }
    }

    private fun handleCandidatesFor403(verb: HttpVerb, path: RestPath, candidates: List<EvaluatedIndividual<RestIndividual>>){

        /*
            we have no idea of the access policy for each user.
            for example, an admin will not get any 403.
            so, we need to check each possible user for which we got a 2xx
         */
        candidates.mapNotNull {
            //first make copy and slice off all after the 2xx
            val index = RestIndividualSelectorUtils.findIndexOfAction(
                it,
                verb,
                path,
                statusGroup = StatusGroup.G_2xx,
                authenticated = true
            )
            if (index < 0) {
                //can this ever happen?
                log.warn("Failed to identify authenticated GET action with 2xx")
                null
            } else {
                val copy = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(it.individual, index)
                copy
            }
        }
            // then, just take 1 test per user
            .distinctBy { it.seeMainExecutableActions().last().auth.name }
            .forEach { ind ->
                //finally, evaluate all of those with a different auth
                val lastCall = ind.seeMainExecutableActions().last()
                val otherUsers = authSettings.getAllOthers(lastCall.auth.name, HttpWsAuthenticationInfo::class.java)

                //try each of them
                otherUsers.forEach { otherAuth ->
                    val copy = ind.copy() as RestIndividual

                    if(lastCall.verb == HttpVerb.PUT){
                        /*
                            a PUT might create the resource, so need to duplicate before changing auth. ie
                            from
                            PUT /x FOO
                            to
                            PUT /x FOO
                            PUT /x BAR
                         */
                        val repeat = lastCall.copy() as RestCallAction
                        copy.addMainActionInEmptyEnterpriseGroup(action = repeat)
                        copy.resetLocalIdRecursively() //TODO what about links?
                        copy.doInitializeLocalId()
                    }
                    copy.seeMainExecutableActions().last().auth = otherAuth
                    org.evomaster.core.Lazy.assert {copy.verifyValidity(); true}

                    val ei = fitness.computeWholeAchievedCoverageForPostProcessing(copy)
                    if(ei != null) {
                        archive.addIfNeeded(ei)
                        val res = ei.evaluatedMainActions().last().result as RestCallResult
                        if(res.getStatusCode() == 403){
                            //we are done
                            return
                        }
                    }
                }
            }
    }

    private fun addForAccessControl() {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongo and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */

        accessControlBasedOnRESTGuidelines()
        //TODO implement access control based on database monitoring
        //accessControlBasedOnDatabaseMonitoring()
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines() {

        // quite a few rules here that can be defined
        handleForbiddenOperationButOKOthers(HttpVerb.DELETE)
        handleForbiddenOperationButOKOthers(HttpVerb.PUT)
        handleForbiddenOperationButOKOthers(HttpVerb.PATCH)

        // getting 404 instead of 403
        handleExistenceLeakage()

        //authenticated, but wrongly getting 401 (eg instead of 403)
        handleNotRecognizedAuthenticated()

        handleForgottenAuthentication()
        //TODO other rules. See FaultCategory
        //etc.
    }


    /**
     * Authenticated user A accesses endpoint X, but get 401 (instead of 403).
     * In theory, a bug. But, could be false positive if A is misconfigured.
     * How to check it?
     * See if on any other endpoint Y we get a 2xx with A.
     * But, maybe Y does not need authentication...
     * so, check if there is any test case for which on Y we get a 401 or 403.
     * if yes, then X is buggy, as should had rather returned 403 for A.
     */
    private fun handleNotRecognizedAuthenticated() {

        mainloop@ for(action in actionDefinitions){

            val suspicious = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                action.verb,
                action.path,
                status = 401,
                authenticated = true
            ).map { RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                it,
                action.verb,
                action.path,
                status = 401,
                authenticated = true
                )
            }.distinctBy { it.seeMainExecutableActions().last().auth.name }

            if(suspicious.isEmpty()){
                continue
            }

            for(target in suspicious) {

                val user = target.seeMainExecutableActions().last().auth.name

                val ok = RestIndividualSelectorUtils.findIndividuals(
                    individualsInSolution,
                    statusGroup = StatusGroup.G_2xx,
                    authenticatedWith = user
                ).map { RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                    it,
                    statusGroup = StatusGroup.G_2xx,
                    authenticatedWith = user)
                }.distinctBy { it.seeMainExecutableActions().last().getName() }

                if(ok.isEmpty()){
                    continue
                }

                /*
                    so, the given suspicious user that got 401 can get a 2xx on an endpoint.
                    has anyone got a 401 or 403 on this endpoint?
                    actually, could even be same user, eg when trying to access resource of
                    another user could get a 403
                */
                for(success in ok){
                    val last = success.seeMainExecutableActions().last()
                    val with401or403 = RestIndividualSelectorUtils.findIndividuals(
                        individualsInSolution,
                        verb = last.verb,
                        path = last.path,
                        statusCodes = listOf(401,403),
                        authenticated = true
                    ).map {
                        RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                            it,
                            verb = last.verb,
                            path = last.path,
                            statusCodes = listOf(401,403),
                            authenticated = true
                        )
                    }
                    if(with401or403.isEmpty()){
                        continue
                    }
                    // if reach here, we got a bug
                    val auth = with401or403.minBy { it.size() }

                    val final = RestIndividualBuilder.merge(auth,success,target)
                    final.modifySampleType(SampleType.SECURITY)
                    final.ensureFlattenedStructure()

                    val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(final)
                    if (evaluatedIndividual == null) {
                        log.warn("Failed to evaluate constructed individual in handleNotRecognizedAuthenticated")
                        continue
                    }

                    val added = archive.addIfNeeded(evaluatedIndividual)
                    assert(added)
                    continue@mainloop
                }
            }
        }
    }


    /**
     * Accessing a protected GET endpoint with id with not authorized should return 403, even if not exists.
     * otherwise, if returning 404, we can find out that the resource does not exist.
     */
    private fun handleExistenceLeakage(){

        val getOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, HttpVerb.GET)

        getOperations.forEach { get ->

            val inds403 = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                HttpVerb.GET,
                get.path,
                status = 403
                )
            if(inds403.isEmpty()){
                return@forEach
            }

            val inds404 = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                HttpVerb.GET,
                get.path,
                status = 404
            )
            if(inds404.isEmpty()){
                return@forEach
            }

            //found the bug.
            val forbidden = inds403.minBy { it.individual.size() }
            val notfound = inds404.maxBy { it.individual.size() }

            //needs slicing to minimize the newly generated test
            val index403 = RestIndividualSelectorUtils.getIndexOfAction(
                forbidden,
                HttpVerb.GET,
                get.path,
                403
            )
            // slice the individual in a way that delete all calls after the chosen verb request
            val first = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(forbidden.individual, index403)

            val index404 = RestIndividualSelectorUtils.getIndexOfAction(
                notfound,
                HttpVerb.GET,
                get.path,
                404
            )
            // slice the individual in a way that delete all calls after the chosen verb request
            val second = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(notfound.individual, index404)

            val final = RestIndividualBuilder.merge(first, second)

            final.modifySampleType(SampleType.SECURITY)
            final.ensureFlattenedStructure()

            val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(final)
            if (evaluatedIndividual == null) {
                log.warn("Failed to evaluate constructed individual in handleExistenceLeakage")
                return@forEach
            }

            //verify if newly constructed individual still find the bug
            val check403 = RestIndividualSelectorUtils.getIndexOfAction(
                evaluatedIndividual,
                HttpVerb.GET,
                get.path,
                403
            )
            val check404 = RestIndividualSelectorUtils.getIndexOfAction(
                evaluatedIndividual,
                HttpVerb.GET,
                get.path,
                404
            )
            //fitness function should have detected the fault
            val faults = (evaluatedIndividual.evaluatedMainActions().last().result as RestCallResult).getFaults()

            if(check403 < 0 || check404 < 0 || faults.none { it.category == DefinedFaultCategory.SECURITY_EXISTENCE_LEAKAGE }){
                //if this happens, it is a bug in the merge... or flakiness
                log.warn("Failed to construct new test showing the 403 vs 404 security leakage issue")
                return@forEach
            }

            val added = archive.addIfNeeded(evaluatedIndividual)
            //if we arrive here, should always be added, because we are creating a new testing target
            assert(added)
        }
    }


    /**
     * Given a target verb like DELETE,
     * here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx) | or possibly via SQL insertions
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenOperationButOKOthers(targetVerb: HttpVerb) {

        /*
    what needs to be done here:
    - check if at least 2 users. if not, nothing to do
    - from schema, check all DELETE operations
    - from archive, search if there is any test with a DELETE returning a 403
    - do that for every different endpoint. There are 2 options:
    -- (1) there is such call,  then
    ---     make a copy of the individual
    ----    do a "slice" and remove all calls after the DELETE call (if any)
    -- (2) there is not. need to create it based on successful create resources with authenticated user
    ---    search for create resource for endpoint of DELETE
    ---    do slice and remove calls after create resource endpoint call
    ---    add a DELETE with different user (verify get a 403)
    ---    make sure that DELETE is bound on some resource path as the create operation
    - append new REST Call action (see mutator classes) for PATCH/PUT with different auth, based
      on action copies from action definitions. should be on same endpoint.
    - need to resolve path element parameters to point to same resolved endpoint as DELETE
    - as PUT/PATCH requires payloads and possible valid query parameters, search from archive an
      existing action that returns 2xx, and copy it to use as starting point
    - execute new test case with fitness function
    - create new testing targets based on status code of new actions
    - add to archive (if needed)
 */

        // Check if at least 2 users. if not, nothing to do
        if (!checkForAtLeastNumberOfAuthenticatedUsers(2)) {
            return
        }

        // From schema, check all operations for target verb X, in order to do that
        // obtain all X operations in the SUT according to the Swagger
        val verbOperations = RestIndividualSelectorUtils.getAllActionDefinitions(actionDefinitions, targetVerb)

        verbOperations.forEach { call ->

            val individualWith403LastCall = findOrCreateTestFor403(call.path, targetVerb)
                ?: return@forEach

            // After having a set of requests in which the last one is a 403 DELETE (for example) call with another user,
            // add a PUT or PATCH (for example) with same user as 403 DELETE
            addWronglySuccessOperation(individualWith403LastCall, targetVerb)
        }
    }



    private fun addWronglySuccessOperation(
        individualWith403LastCall: RestIndividual,
        verb: HttpVerb
    ) {
        //at this point, the individual we are building should end with a 403 call.
        //however, recall that, as the fitness function is not executed yet,
        //we cannot be sure of returned status code
        val lastAction = individualWith403LastCall.seeMainExecutableActions().last()
        assert(lastAction.verb == verb)

        val actions = HttpVerb.otherWriteOperationsOnSameResourcePath(verb)
            .mapNotNull {
                RestIndividualSelectorUtils.findAction(
                    individualsInSolution,
                    it,
                    lastAction.path,
                    statusGroup = StatusGroup.G_2xx
                )?.copy() as RestCallAction?
            }


        actions.forEach {
            it.resetLocalIdRecursively()
            //make sure using same auth
            it.auth = lastAction.auth
            it.usePreviousLocationId = lastAction.usePreviousLocationId
            it.bindBasedOn(lastAction.path, lastAction.parameters.filterIsInstance<PathParam>(), null)

            //create new individual where this action on same path and auth that led to 403 is added
            val finalIndividual = individualWith403LastCall.copy() as RestIndividual
            finalIndividual.addResourceCall(
                restCalls = RestResourceCalls(
                    actions = mutableListOf(it),
                    sqlActions = listOf()
                )
            )

            finalIndividual.seeMainExecutableActions().filter { it.verb == HttpVerb.PUT || it.verb == HttpVerb.POST }.forEach{
                it.saveCreatedResourceLocation = true
            }
            finalIndividual.fixResourceForwardLinks()

            finalIndividual.modifySampleType(SampleType.SECURITY)
            finalIndividual.ensureFlattenedStructure()

            val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(finalIndividual)
            if (evaluatedIndividual == null) {
                log.warn("Failed to evaluate constructed individual in security testing phase")
                return@forEach
            }
            //at this point, we have a new evaluated individual.
            //if there was any security issue, that should had been detected in fitness function!!!
            //not here.
            //this is because otherwise phases like minimization would be broken.

            //anyway, let's verify indeed second last action if a 403 target verb. otherwise, it is a problem
            val ema = evaluatedIndividual.evaluatedMainActions()

            val n = evaluatedIndividual.individual.seeMainExecutableActions().size
            if(ema.size != n){
                log.warn("Failed to build security test. Premature stopping of HTTP call sequence")
                return@forEach
            }

            val secondLast = ema[ema.size - 2]
            val secondLastAction = secondLast.action
            val secondLastResult = secondLast.result
            if(secondLastAction !is RestCallAction || secondLastResult !is RestCallResult) {
                //shouldn't really ever happen...
                //TODO should refactor code to enforce generics in subclasses
                log.warn("Wrong type: non-REST action/result")
                return@forEach
            }
            if (secondLastAction.verb != verb ||  secondLastResult.getStatusCode() != 403) {
                log.warn("Issue with constructing evaluated individual. Expected a 403 $verb," +
                        " but got: ${secondLastResult.getStatusCode()} ${secondLastAction.verb}")
                return@forEach
            }

            /*
                FIXME: isn't this wrong??? ie, should be done in fitness function
             */
            val scenarioId = idMapper.handleLocalTarget("security:forbidden$verb:${lastAction.path}")
            evaluatedIndividual.fitness.updateTarget(scenarioId, 1.0)

            // add the evaluated individual to the archive
            val added = archive.addIfNeeded(evaluatedIndividual)
            //if we arrive here, should always be added, because we are creating a new testing target
            assert(added)
        }
    }


    /**
     * Check if there is a test case with a 403 and another one with a 200 without authentication.
     * To check this, there must be a resource. It can either be newly created or already exist,
     * such as during initialization. While the user who created this
     * resource can access it (200), the other user cannot (403). However, if a 200 status code is
     * returned when attempting to access the same resource without sending the authorization header,
     * it indicates that authorization checks are wrongly ignored if no auth info is set.
     * Example:
     * POST /resources/ AUTH1 -> 201 (location header: /resources/42/)
     * GET /resources/42/ AUTH1 -> 200
     * GET /resources/42/ AUTH2 -> 403
     * GET /resources/42/ NOAUTH -> 200
     */
    private fun handleForgottenAuthentication() {
        actionDefinitions.forEach { op ->
            val ind403or401 = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                statusCodes = listOf(401,403)
            )

            if (ind403or401.isEmpty()) {
                return@forEach //there is not any protected resource for this path/verb.
            }

            val i2xx = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                statusGroup = StatusGroup.G_2xx,
                authenticated = false
            )

            if(i2xx.isNotEmpty()){
                // we have a 2xx without auth, so we can create a test case
                // we can just take the smallest 403 or 401 and the smallest 2xx

                // FIXME: mocked external services might return 401/403 or 2xx without auth.
                // in this case, we couldn't merge them because of "child already present" error.
                // we need to fix this.

                val first = ind403or401.minBy { it.individual.size() }

                val actionIndexFirst = RestIndividualSelectorUtils.findIndexOfAction(
                    first,
                    op.verb,
                    op.path,
                    statusCodes = listOf(401,403)
                )

                val firstSliced = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                    first.individual,
                    actionIndexFirst
                )


                val second = i2xx.minBy { it.individual.size() }.copy()
                val actionIndexSecond = RestIndividualSelectorUtils.findIndexOfAction(
                    second,
                    op.verb,
                    op.path,
                    statusGroup = StatusGroup.G_2xx,
                    authenticated = false
                )
                val secondSliced = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                    second.individual,
                    actionIndexSecond
                )

                secondSliced.removeHostnameResolutionAction(firstSliced.seeAllActions().filter {
                    it is HostnameResolutionAction
                } as List<HostnameResolutionAction>)

                val finalIndividual = RestIndividualBuilder.merge(
                    firstSliced,
                    secondSliced
                )

                finalIndividual.modifySampleType(SampleType.SECURITY)
                finalIndividual.ensureFlattenedStructure()
                org.evomaster.core.Lazy.assert { finalIndividual.verifyValidity(); true }
                val ei = fitness.computeWholeAchievedCoverageForPostProcessing(finalIndividual)
                if (ei != null) {
                    val added = archive.addIfNeeded(ei)
                    assert(added)
                }

                return@forEach
            }

            // if we arrive here, we have a 403 or 401, but no 2xx without auth. Now, we can try to create one
            val candidates = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                op.verb,
                op.path,
                statusGroup = StatusGroup.G_2xx,
                authenticated = true
            )
            if (candidates.isEmpty()) {
                return@forEach
            }

            candidates.forEach { ind ->

                // we have a candidate individual with a 2xx on the same path/verb
                // we need to copy the last action, which is the one that returns 2xx
                val action2xx = RestIndividualSelectorUtils.findIndexOfAction(
                    ind,
                    op.verb,
                    op.path,
                    statusGroup = StatusGroup.G_2xx,
                )

                val slicedIndividual = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                    ind.individual,
                    action2xx
                )

                val copyLast = slicedIndividual.seeMainExecutableActions().last().copy() as RestCallAction
                val copyNoAuthLast = copyLast.copy() as RestCallAction

                copyLast.resetLocalIdRecursively()
                copyNoAuthLast.resetLocalIdRecursively()


                val otherUsers = authSettings.getAllOthers(copyLast.auth.name, HttpWsAuthenticationInfo::class.java)

                otherUsers.forEach { other ->
                    val finalIndividual = slicedIndividual.copy() as RestIndividual

                    // we need to set the auth for the last action to the other user
                    copyLast.auth = other
                    // and for the no auth one, we set it to NoAuth
                    copyNoAuthLast.auth = HttpWsNoAuth()

                    finalIndividual.addResourceCall(
                        restCalls = RestResourceCalls(
                            actions = mutableListOf(copyLast, copyNoAuthLast),
                            sqlActions = listOf()
                        )
                    )
                    finalIndividual.seeMainExecutableActions()
                        .filter { it.verb == HttpVerb.PUT || it.verb == HttpVerb.POST }.forEach {
                        it.saveCreatedResourceLocation = true
                    }
                    finalIndividual.fixResourceForwardLinks()

                    finalIndividual.modifySampleType(SampleType.SECURITY)
                    finalIndividual.ensureFlattenedStructure()
                    org.evomaster.core.Lazy.assert { finalIndividual.verifyValidity(); true }

                    val ei = fitness.computeWholeAchievedCoverageForPostProcessing(finalIndividual)
                    if (ei != null) {
                        archive.addIfNeeded(ei)
                    }
                }
            }
        }
    }



    /**
     * @return a test where last action is for given path and verb returning 403.
     * null if failed.
     */
    private fun findOrCreateTestFor403(
        path: RestPath,
        verb: HttpVerb,
    ): RestIndividual? {
        // from archive, search if there is any test with given verb returning a 403
        val existing403 = RestIndividualSelectorUtils.findIndividuals(
            individualsInSolution,
            verb,
            path,
            403,
            authenticated = true
        )

        // if there is such an individual
        if (existing403.isNotEmpty()) {

            // current individual in the list of existing 403. Since the list is not empty,
            // we can just get the first item
            val currentIndividualWith403 = existing403[0]

            val actionIndex = RestIndividualSelectorUtils.getIndexOfAction(
                currentIndividualWith403,
                verb,
                path,
                403
            )

            // slice the individual in a way that delete all calls after the chosen verb request
            return RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
                currentIndividualWith403.individual,
                actionIndex
            )
        }

        // there is not. need to create it based on successful create resources with authenticated user
        // but, first let's check if we can have any successfully delete action

        /*
            We have a "verb" (eg, a DELETE) path in form for example
            /users/{id}
            and want to get a _resolved_ creation action (either PUT or POST) for it.
            The new "verb" we are going to create must point to the same resolved action.
            But "verb" could have query parameters and possibly body payloads... all with
            constraints that must be satisfied.
            So we cannot easily just create it from scratch.
            Need to re-use an existing one, if any.
        */
        var successIndividuals = RestIndividualSelectorUtils.findIndividuals(
            individualsInSolution,
            verb,
            path,
            statusGroup = StatusGroup.G_2xx,
        )

        if (successIndividuals.isEmpty()) {
            /*
                This needs a bit of explanation.
                We want to get an action that works, without failed constraint validation on
                query parameters or body payloads.
                Ideally, a 2xx would do.
                But what if we could not create any because they all fail to point to an existing
                resource? if 404, could still be fine, as then we link it to creation operation
             */
            successIndividuals = RestIndividualSelectorUtils.findIndividuals(
                individualsInSolution,
                verb,
                path,
                status = 404
            )
        }
        if (successIndividuals.isEmpty()) {
            //no point trying to create a successful action directly, if none was evolved at all...
            //as likely would fail as well here
            return null
        }

        //start from base test in which resource is created
        val (creationIndividual, creationEndpoint) = RestIndividualSelectorUtils
            .findIndividualWithEndpointCreationForResource(
                individualsInSolution,
                path,
                true
            ) ?: return null

        // find the index of the creation action
        val actionIndexForCreation = creationIndividual.individual.getActionIndex(
            creationEndpoint.verb,
            creationEndpoint.path
        )
        val creationAction = creationIndividual.individual.seeMainExecutableActions()[actionIndexForCreation]
        assert(creationAction.auth !is NoAuth)

        //we don't need anything after the creation action
        val sliced = RestIndividualBuilder.sliceAllCallsInIndividualAfterAction(
            creationIndividual.individual,
            actionIndexForCreation
        )

        // from a success target operation individual,
        // create a new target action on same path,
        // but then change auth
        val targetInd = successIndividuals.first().individual
        val targetActionIndex = targetInd.getActionIndex(verb, path)
        val targetAction = targetInd.seeMainExecutableActions()[targetActionIndex].copy() as RestCallAction
        assert(targetAction.verb == verb && targetAction.path.isEquivalent(path))
        targetAction.resetLocalIdRecursively()
        targetAction.auth =
            authSettings.getDifferentOne(creationAction.auth.name, HttpWsAuthenticationInfo::class.java, randomness)

        //    Bind the creation operation and new target action based on their path
        linkAndBindActions(creationAction, targetAction)

        //finally, add the target action to the test including the creation of the resource
        sliced.addResourceCall(
            restCalls = RestResourceCalls(
                actions = mutableListOf(targetAction),
                sqlActions = listOf()
            )
        )

        return sliced
    }

    private fun linkAndBindActions(
        creationAction: RestCallAction,
        targetAction: RestCallAction
    ) {
        CreateResourceUtils.linkDynamicCreateResource(creationAction, targetAction)
        if (creationAction.path.isEquivalent(targetAction.path)) {
            targetAction.bindBasedOn(creationAction.path, creationAction.parameters.filterIsInstance<PathParam>(), null)
        }
    }


    /**
     * Wrong Authorization
     *
     * User A, access endpoint X, but get 401 (instead of 403).
     * In theory, a bug. But, could be false positive if A is misconfigured.
     * How to check it?
     * See if on any other endpoint Y we get a 2xx with A.
     * But, maybe Y does not need authentication...
     * so, check if there is any test case for which on Y we get a 401 or 403.
     * if yes, then X is buggy, as it should rather have returned 403 for A.
     * This seems to actually happen for familie-ba-sak, new NAV SUT.
     */
    fun handleUnauthorizedInsteadOfForbidden() {

    }

    private fun checkForAtLeastNumberOfAuthenticatedUsers(numberOfUsers: Int): Boolean {

        if (authSettings.size(HttpWsAuthenticationInfo::class.java) < numberOfUsers) {
            LoggingUtil.getInfoLogger()
                .debug("Security test for this method requires at least $numberOfUsers authenticated users")
            return false
        }

        return true
    }

}
