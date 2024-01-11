package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import javax.annotation.PostConstruct

import org.apache.http.HttpStatus

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.NoAuth
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.remote.service.RemoteControllerImplementation

import org.evomaster.core.search.*
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler



/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    /**
     * archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>


    @Inject
    private lateinit var sampler: RestSampler

    /**
     * Remote controller which is used in white-box testing
     */
    @Inject
    lateinit var remoteControllerImplementation: RemoteControllerImplementation

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>


    /**
     * Individuals in the solution
     */
    private lateinit var individualsInSolution : List<EvaluatedIndividual<RestIndividual>>

    /**
     * Authentication objects made as a HashMap for identifying authentication information used
     * in an efficient manner
     */
    private var authenticationInfoMap = mutableMapOf<String, AuthenticationDto>()

    @PostConstruct
    private fun postInit(){

        // get action definitions
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>

        //TODO init authenticationInfoMap
        sampler.authentications()
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase() : Solution<RestIndividual>{

        // extract individuals from the archive
        val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution()
        individualsInSolution =  archivedSolution.individuals


        //  call sutInfo Once here and pass it as a parameter for all cases.
        val sutInfo : SutInfoDto? = remoteControllerImplementation.getSutInfo()

        //TODO in future will need to get it from EMConfig as well, to do Black-Box testing

        val authInfo = mutableListOf<AuthenticationDto>()
        sutInfo?.infoForAuthentication?.forEach { authInfo.add(it) }

        for (authInformation : AuthenticationDto in authInfo) {
            authenticationInfoMap[authInformation.toString()] = authInformation
        }

        // we can see what is available from the schema, and then check if already existing a test for it in archive
        addForAccessControl(authInfo)

        return archive.extractSolution()
    }

    private fun addForAccessControl(authInfo: List<AuthenticationDto>) {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongo and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */

        accessControlBasedOnRESTGuidelines(authInfo)
        //TODO implement access control based on database monitoring
        //accessControlBasedOnDatabaseMonitoring()
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines(authInfo: List<AuthenticationDto>) {

        // quite a few rules here that can be defined
        handleForbiddenDeleteButOkPutOrPatch(authInfo)
        //TODO other rules
    }

    private fun checkRESTSemantics(authInfo: List<AuthenticationDto>) {

        // TODO add tests for checking REST semantics such as
        // a resource should not be accessible after deletion.
        handleResourceReadableAfterDelete(authInfo)

    }

    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenDeleteButOkPutOrPatch(authInfo: List<AuthenticationDto>) {

        /*
            here, need to go through archive, for all successful create resources with authenticated user.
            for each of them, do a DELETE with a new user.
            verify if we get 403.
            if so, try a PUT and PATCH.
            when doing this, check from archive for test already doing it, as payloads and params of PUT/PATCH
            might have constraints.
            if 2xx, create new fault definition.
            add to archive the new test
         */

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
            - append new REST Call action (see mutator classes) for PATCH/PUT with different auth, based
              on action copies from action definitions. should be on same endpoint.
            - need to resolve path element parameters to point to same resolved endpoint as DELETE
            - as PUT/PATCH requires payloads and possible valid query parameters, search from archive an
              existing action that returns 2xx, and copy it to use as starting point
            - execute new test case with fitness function
            - create new testing targets based on status code of new actions
            - add to archive (if needed)
         */


        // check that there are at least two users, using the method getInfoForAuthentication()
        if (authInfo.size <= 1) {

            // nothing to test if there are not at least 2 users
            LoggingUtil.getInfoLogger().debug(
                "Security test handleForbiddenDeleteButOkPutOrPatch requires at least 2 authenticated users")
            return
        }

        // obtain DELETE operations in the SUT according to the swagger
        val deleteOperations = getAllActionDefinitions(HttpVerb.DELETE)

        //do we already have existing test cases returning 403 for it?
        // check if there is a DELETE operation with the status code 403 - which means forbidden


        // check delete operations for to see if there is an existing 403
        deleteOperations.forEach { delete ->

            // variable initialization moved to here
            lateinit var existing403 : List<EvaluatedIndividual<RestIndividual>>


            //TODO check if there is either a PUT or PATCH on such endpoint

            // obtain individuals which result in successful creation using PUT or successful PATCH
            // which have the same path as the DELETE request
            var individualsWithSuccessfulPut : List<EvaluatedIndividual<RestIndividual>> =
                getIndividualsWithActionAndStatus(individualsInSolution, HttpVerb.PUT,
                delete.path, HttpStatus.SC_CREATED)


            var individualsWithSuccessfulPatch : List<EvaluatedIndividual<RestIndividual>> =
                getIndividualsWithAction(individualsInSolution, HttpVerb.PATCH, delete.path)



            // Authentication Objects used in PUT or PATCH
            lateinit var authenticationObjectsForPutOrPatch : List<HttpWsAuthenticationInfo>

            // Authentication DTO which has not been used for creating a resource
            var authenticationNotUsedInCreation : AuthenticationDto?

            // existing PUT action
            var existingPutAction : RestCallAction? = null

            // new authentication object which represents the same endpoint with a new user
            var newAuth : HttpWsAuthenticationInfo? = null


            // if there is an existing PUT, find the AuthenticationDto object used for the existing PUT.
            if (individualsWithSuccessfulPut.isNotEmpty()) {
                authenticationObjectsForPutOrPatch = identifyAuthenticationInformationUsedForIndividuals(individualsWithSuccessfulPut)
            }
            // if there is not a PUT request with the same endpoint, check for PATCH request
            else if (individualsWithSuccessfulPatch.isNotEmpty()) {
                authenticationObjectsForPutOrPatch = identifyAuthenticationInformationUsedForIndividuals(individualsWithSuccessfulPatch)
            }
            // if there is no PUT or PATCH for the endpoint, this is a special case.
            else {
                //TODO we could not find PUT or PATCH for the endpoint, this case should be handled as a special case
            }

            // So far, we found an existing PUT or PATCH along with its authentication information.
            // create an individual containing a DELETE request and whose authentication information is created
            // from a different AuthenticationDto than the existing PUT
            existing403 = getIndividualsWithActionAndStatus(individualsInSolution, HttpVerb.DELETE, delete.path, 403)

            if(existing403.isEmpty()){

                //we do not have such call in evolved tests. we need to create it by ourself
                //TODO implement it

                // create copy of the delete call
                val restActionForbiddenDelete = delete.copy() as RestCallAction

                // find the AuthenticationDto which is different from authenticationObjectsForPutOrPatch
                authenticationNotUsedInCreation =
                    findAuthenticationDtoDifferentFromUsedAuthenticationObjects (authInfo, authenticationObjectsForPutOrPatch)

                // Create a new HttpWsAuthentication based on authenticationDto not used by PUT
                val newHeaders = mutableListOf<AuthenticationHeader>()
                newHeaders.add(AuthenticationHeader("Authorization",
                        authenticationNotUsedInCreation?.headers?.get(0)?.value.toString()
                ))

                // new authentication object
                newAuth = HttpWsAuthenticationInfo("newInfo",newHeaders, null, null)

                // changing the authentication information of the forbidden delete
                restActionForbiddenDelete.auth = newAuth as HttpWsAuthenticationInfo

                // this individual contains two actions which are successful PUT to create the resource
                // and a forbidden delete to
                val actionList = emptyList<RestCallAction>().toMutableList()

                // existing PUT action
                existingPutAction = findActionFromIndividuals(individualsWithSuccessfulPut, HttpVerb.PUT, delete.path)

                // also change the object to be deleted to make sure all operations are done on the same object
                changePathParameter(restActionForbiddenDelete, getPathParameter(existingPutAction as RestCallAction))

                // add the successful existing PUT action
                actionList.add(existingPutAction as RestCallAction)

                // add the forbidden DELETE action
                actionList.add(restActionForbiddenDelete)

                // create an individual based on those actions
                val createdIndividual = RestIndividual(actionList, SampleType.PREDEFINED)

                val restFitness : FitnessFunction<RestIndividual> = RestFitness()

                val newEvaluatedIndividual = restFitness.calculateCoverage(createdIndividual)

                /*

                // create results for the new individual
                val results: MutableList<RestCallResult> = mutableListOf()

                // PUT action results in SC_CREATED
                val resultOfPut = RestCallResult()
                resultOfPut.setStatusCode(HttpStatus.SC_CREATED)

                // DELETE action results in SC_FORBIDDEN
                val resultOfDelete = RestCallResult()
                resultOfDelete.setStatusCode(HttpStatus.SC_FORBIDDEN)

                // add results of PUT and DELETE
                results.add(resultOfPut)
                results.add(resultOfDelete)

                // Based on action and results, create EvaluatedIndividual
                // fitness value
                val fv = FitnessValue(0.0)

                // ensure all genes are initialized before
                createdIndividual.seeGenes().forEach { if (!it.initialized) {it.markAllAsInitialized()} }

                // new evaluated individual based on results and the created individual
                val newEvaluatedIndividual = EvaluatedIndividual(fv, createdIndividual, results)



                 */
                // replace the list containing the newly created individual to with existing403
                val newList = mutableListOf<EvaluatedIndividual<RestIndividual>>()
                if (newEvaluatedIndividual != null) {
                    newList.add(newEvaluatedIndividual)
                }

                // modify existing403
                existing403 = newList

            }

            //there could be several test cases for that DELETE operation... we just take the shortest test
            val chosenExisting403 : EvaluatedIndividual<RestIndividual>  = existing403.minByOrNull { it.individual.size() }!!

            // create a copy of the individual
            val copy = chosenExisting403.individual.copy()

            // slice all restCallAction and RestCallResults before forbidden DELETE
            sliceIndividual(copy as RestIndividual, HttpVerb.DELETE, delete.path, 403)

            //TODO add PUT/PATH with different auth

            // final actions of the REST call
            val finalActionList = emptyList<RestCallAction>().toMutableList()
            // final results of the rest call
            val finalResults: MutableList<RestCallResult> = mutableListOf()

            // resultIndex is needed since some actions may be sliced, which means their results should be sliced too
            var resultIndex = 0

            for( act : RestCallAction in copy.seeMainExecutableActions()) {
                finalActionList.add(act)
                finalResults.add(chosenExisting403.seeResults()[resultIndex] as RestCallResult)
                resultIndex += 1
            }

            // add PUT or PATCH with different user to final action list
            val unauthorizedPut = existingPutAction?.copy() as RestCallAction
            unauthorizedPut.auth = newAuth as HttpWsAuthenticationInfo

            val unauthorizedPutResult = RestCallResult()
            unauthorizedPutResult.setStatusCode(HttpStatus.SC_NO_CONTENT)

            finalActionList.add(unauthorizedPut)
            finalResults.add(unauthorizedPutResult)

            // create individual and evaluatedIndividual
            val finalIndividual = RestIndividual(finalActionList, SampleType.PREDEFINED)

            val fv = FitnessValue(0.0)

            // ensure all genes are initialized before
            finalIndividual.seeGenes().forEach { if (!it.initialized) {it.markAllAsInitialized()} }

            val finalTestCase = EvaluatedIndividual(fv, finalIndividual, finalResults)

            // find all test targets used in archive so that we create a test target that does not exist
            val testTargets = findAllTestTargetsVisitedByIndividuals()

            // cover a fake test target, whose index is more than indices of
            finalTestCase.fitness.coverTarget(testTargets.max() + 10)

            // add the new test to archive. It should succeed since we added a testing target that does not exist.
            archive.addIfNeeded(finalTestCase)

        }

        // get all endpoints used in tests
        // for each endpoint
        // check if there is a successful DELETE request
        // if there is, make a clone of the individual and slice all parts before DELETE
        // Place PUT / PATCH instead of DELETE with another user other than the authenticated user
        // for this, first search for a successful PUT / PATCH from the archive since its payload may be needed
        // if the command succeeds, that's a security issue.
        // if there is no DELETE
        // search for a successful PUT/PATCH which creates the resource,
        // try to delete the resource with the owner, which should succeed.
        // create the resource again with the owner
        // try a DELETE request with another user which should fail
        // try a PUT/ PATCH request with another user, if it succeeds, a security issue

    }

    /*
    If a resource is DELETED successfully, it should not be accessible anymore even
    by the same user who has created the resource. If it is, this is a security issue

        What needs to be done:
        (1) For each DELETE operations, find a successful DELETE
        (2) Find a GET operation corresponding to the successful DELETE with
        (3) Make sure GET has the same path parameter
        (4) Call GET operation with the same authenticated user
        (5) If this succeeds, there is a security issue

     */
    private fun handleResourceReadableAfterDelete(authInfo: List<AuthenticationDto>) {

        // find all delete operations
        val putOperations = getIndividualsWithActionAndStatus(individualsInSolution,HttpVerb.PUT, HttpStatus.SC_CREATED)

        var existingPutAction : RestCallAction?
        var deleteFromExistingPut : RestCallAction
        var getFromExistingDelete : RestCallAction
        var newIndividual : RestIndividual

        // for each DELETE operation
        putOperations.forEach { putOp ->

            var newListOfActions = mutableListOf<RestCallAction>()

            // get the successful delete action from the individual
            existingPutAction = findActionFromIndividualsBasedOnVerbAndStatus(putOp, HttpVerb.PUT, HttpStatus.SC_CREATED)

            if (existingPutAction != null) {

                // create a GET request from the existing delete action with the same authentication and same path
                deleteFromExistingPut = createCopyOfActionWithDifferentVerbOrUser(
                    "deleteFromPut", existingPutAction!!,
                    HttpVerb.DELETE, existingPutAction!!.auth
                )

                changePathParameter(deleteFromExistingPut, getPathParameter(existingPutAction!!))

                // create also GET from existing PUT with no Authentication
                getFromExistingDelete = createCopyOfActionWithDifferentVerbOrUser("getFromDelete",
                    deleteFromExistingPut!!, HttpVerb.GET, NoAuth())
                changePathParameter(getFromExistingDelete, getPathParameter(existingPutAction!!))

                // list of actions
                newListOfActions.add(existingPutAction!!)
                newListOfActions.add(deleteFromExistingPut)
                newListOfActions.add(getFromExistingDelete)

                // individual from the list of actions
                newIndividual = RestIndividual(newListOfActions, SampleType.PREDEFINED)

                // results
                val results: MutableList<RestCallResult> = mutableListOf()

                // PUT action results in SC_CREATED
                val resultOfPut = RestCallResult()
                resultOfPut.setStatusCode(HttpStatus.SC_CREATED)

                // DELETE action results in SC_OK
                val resultOfDelete = RestCallResult()
                resultOfDelete.setStatusCode(HttpStatus.SC_OK)

                // GET Action
                val resultOfGet = RestCallResult()
                resultOfGet.setStatusCode(HttpStatus.SC_OK)

                results.add(resultOfPut)
                results.add(resultOfDelete)
                results.add(resultOfGet)

                // ensure all genes are initialized before
                newIndividual.seeGenes().forEach { if (!it.initialized) {it.markAllAsInitialized()} }

                // new evaluated individual based on results and the created individual
                val newEvaluatedIndividual = EvaluatedIndividual(FitnessValue(0.0), newIndividual, results)

                // find all test targets used in archive so that we create a test target that does not exist
                val testTargets = findAllTestTargetsVisitedByIndividuals()

                // cover a fake test target, whose index is more than indices of
                newEvaluatedIndividual.fitness.coverTarget(testTargets.max() + 10)

                // add the new test to archive. It should succeed since we added a testing target that does not exist.
                archive.addIfNeeded(newEvaluatedIndividual)

            }
        }
    }

    private fun findActionFromIndividualsBasedOnVerbAndStatus(individual: EvaluatedIndividual<RestIndividual>, actionVerb: HttpVerb, actionStatus: Int): RestCallAction? {


        individual.evaluatedMainActions().forEach { currentAct ->

            val act = currentAct.action as RestCallAction
            val res = currentAct.result as RestCallResult

            if ( (res.getStatusCode() == actionStatus) && act.verb == actionVerb)  {

                return act

            }
        }

        return null
    }

    /*
    Find individuals containing a certain action and STATUS
     */
    private fun getIndividualsWithActionAndStatus(individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
                                                  verb: HttpVerb, statusCode: Int):
    List<EvaluatedIndividual<RestIndividual>> {

        val individualsList = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        individualsInSolution.forEach { ind ->
            val actions = ind.evaluatedMainActions()

            val successfulDeleteContained = false

            for (a in actions) {

                val act = a.action as RestCallAction
                val res = a.result as RestCallResult


                if ( (res.getStatusCode() == statusCode) && act.verb == verb)  {

                    if (!individualsList.contains(ind)) {
                        individualsList.add(ind)
                    }
                }
            }
        }

        return individualsList
    }

    /*
    This function searches for AuthenticationDto object in authInfo
    which is not utilized in authenticationObjectsForPutOrPatch
     */
    private fun findAuthenticationDtoDifferentFromUsedAuthenticationObjects(authInfo: List<AuthenticationDto>,
                                                                            authenticationObjectsForPutOrPatch: List<HttpWsAuthenticationInfo>):
            AuthenticationDto? {

        // the AuthenticationDto object which has not been used
        var authenticationDtoNotUsed : AuthenticationDto? = null

        for (firstAuth : AuthenticationDto in authInfo) {

            var notUsedInAny  = true

            // compare AuthenticationDto with HttpWsAuthenticationInfo
            for (secondAuth : HttpWsAuthenticationInfo in authenticationObjectsForPutOrPatch) {

                if ( firstAuth.headers[0].value == secondAuth.headers[0].value ) {
                    notUsedInAny = false
                }

            }

            // if it is not used in any HttpWsAuthenticationInfo, then the authentication header
            // which has not been used has been found.
            if (notUsedInAny) {
                authenticationDtoNotUsed = firstAuth
            }

        }

        return authenticationDtoNotUsed

    }

    /*
    This function finds the list of testing targets visited by an individual. We utilize this function in order to
    ensure that a newly generated test case gets added into the archive.
     */
    private fun findAllTestTargetsVisitedByIndividuals() : List<Int> {

        // list of targets
        val listOfTargets = mutableListOf<Int>()

        // for each individual in the solution
        for (ind in this.archive.extractSolution().individuals) {

            // find all reached targets
            for (t in ind.fitness.reachedTargets()) {
                listOfTargets.add(t)
            }

        }

        // result is a list of test targets visited
        return listOfTargets

    }

    /*
    This method identifies a specific action in an individual. It is used to transform an individual containing
    one action to RestCallAction
     */
    private fun findActionFromIndividuals(individualList: List<EvaluatedIndividual<RestIndividual>>,
                                          verb: HttpVerb, path: RestPath): RestCallAction? {

        // search for RESTCall action in an individual.
        var foundRestAction : RestCallAction? = null

        for (ind : EvaluatedIndividual<RestIndividual> in individualList) {

            for (act : RestCallAction in ind.individual.seeMainExecutableActions()) {

                if (act.verb == verb && act.path == path) {
                    foundRestAction = act
                }

            }

        }

        // the action that has been found
        return foundRestAction
    }



    /*
    This method obtains HttpWsAuthenticationInfo objects from list of individuals.
     */
    private fun identifyAuthenticationInformationUsedForIndividuals(listOfIndividuals: List<EvaluatedIndividual<RestIndividual>>)
    : List<HttpWsAuthenticationInfo>{

        val listOfAuthenticationInfoUsedInIndividuals = mutableListOf<HttpWsAuthenticationInfo>()
        val listOfResults = mutableListOf<Any>()

        // for each individual
        for (ind : EvaluatedIndividual<RestIndividual> in listOfIndividuals) {

            for (child : StructuralElement in ind.individual.getViewOfChildren() ){

                listOfResults.clear()

                // identify HttpWsAuthenticationInfo objects used in the individual
                recursiveTreeTraversalForFindingInformationForItem(child,
                    "org.evomaster.core.problem.rest.RestCallAction",
                    listOfResults
                )

                // for each item in listOfResults which is a list of RestCallAction objects, identify authentication
                for( item : Any in listOfResults) {

                    listOfAuthenticationInfoUsedInIndividuals.add((item as RestCallAction).auth)
                }
            }
        }

        // return the list of AuthenticationInfo objects
        return listOfAuthenticationInfoUsedInIndividuals
    }

    /*
     * Remove all calls AFTER the given call.
     */
    // TODO add checking status code as well
    private fun sliceIndividual(individual: RestIndividual, verb: HttpVerb, path: RestPath, statusCode: Int) {

        // Find the index of the action
        var index = 0
        var found = false
        val actions = individual.seeMainExecutableActions()
        var currentAction : RestCallAction

        while (!found) {

            currentAction = actions[index]

            if ( currentAction.verb == verb &&
                currentAction.path == path ) {
                found = true
            }
            else {
                index += 1
            }
        }

        if (found) {
            // delete all calls after the index
            for (item in index + 1 until actions.size) {
                individual.removeMainExecutableAction(index + 1)
            }
        }


    }


    private fun getIndividualsWithActionAndStatus(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusCode: Int
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction
                val r = ea.result as RestCallResult

                a.verb == verb && a.path.isEquivalent(path) && r.getStatusCode() == statusCode
            }
        }
    }

    private fun getIndividualsWithAction(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction

                a.verb == verb && a.path.isEquivalent(path)
            }
        }
    }

    private fun getAllActionDefinitions(verb: HttpVerb): List<RestCallAction> {
        return actionDefinitions.filter { it.verb == verb }
    }


    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun oldHandleForbiddenDeleteButOkPutOrPatch(authInfo: List<AuthenticationDto>) {
        //TODO("Not yet implemented")

        /*
            check if at least 2 users.
            here, need to go through archive, for all successful create resources with authenticated user.
            for each of them, do a DELETE with a new user.
            verify if we get 403.
            if so, try a PUT and PATCH.
            when doing this, check from archive for test already doing it, as payloads and params of PUT/PATCH
            might have constraints.
            if 2xx, create new fault definition.
            add to archive the new test
         */

        /*
            what needs to be done here:
            - from archive, search if there is any test with a DELETE returning a 2xx
            - do that for every different endpoint
            - make a copy of the individual
            - do a "slice" and remove all calls after the DELETE call (if any)
            - append new REST Call action (see mutator classes) for PATCH/PUT with different auth, based
              on action copies from action definitions. should be on same endpoint.
            - need to resolve path element parameters to point to same resolved endpoint as DELETE
            - as PUT/PATCH requires payloads and possible valid query parameters, search from archive an
              existing action that returns 2xx, and copy it to use as starting point
            - execute new test case with fitness function
            - create new testing targets based on status code of new actions
         */


        // check that there are at least two users, using the method getInfoForAuthentication()
        if (authInfo.size <= 1) {
            // nothing to test if there are not at least 2 users

            LoggingUtil.getInfoLogger().debug("Security test handleForbiddenDeleteButOkPutOrPatch requires at" +
                    "least 2 authenticated users")
            return
        }

        // get all endpoints used in tests


        // for each endpoint

        // check if there is a successful DELETE request

        // if there is, make a clone of the individual and slice all parts before DELETE

        // Place PUT / PATCH instead of DELETE with another user other than the authenticated user
        // for this, first search for a successful PUT / PATCH from the archive since its payload may be needed

        // if the command succeeds, that's a security issue.

        // if there is no DELETE

        // search for a successful PUT/PATCH which creates the resource,
        // try to delete the resource with the owner, which should succeed.
        // create the resource again with the owner
        // try a DELETE request with another user which should fail
        // try a PUT/ PATCH request with another user, if it succeeds, a security issue



        // from the archive, check if any individual has a DELETE with 2xx as REST call action
        val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution()

        // get all endpoints used in tests

        var individualContainingSuccessfulPut : EvaluatedIndividual<RestIndividual>? = null
        var putIndexInEndpoint : Int = -1
        val allEndpoints = mutableListOf<String?>()


        // for each individual, get endpoints from the individual
        for (ind : EvaluatedIndividual<RestIndividual> in archivedSolution.individuals) {

            val actions = ind.individual.seeMainExecutableActions()
            val results = ind.seeResults(actions)
            var actionIndex : Int
            var currentActionResult : ActionResult
            var currentResultStatusCode : String?
            var currentEndpoint : String?

            for (act: RestCallAction in actions) {

                // index of the action in the array, needed for accessing the result
                actionIndex = actions.indexOf(act)

                currentActionResult = results[actionIndex]
                currentResultStatusCode = currentActionResult.getResultValue("STATUS_CODE")
                currentEndpoint = getEndPointFromAction(act)
                // check if the action had verb delete and status success
                if (act.verb == HttpVerb.PUT && (currentResultStatusCode == HttpStatus.SC_CREATED.toString())) {
                    individualContainingSuccessfulPut = ind
                    putIndexInEndpoint = actionIndex
                }

                allEndpoints.add(currentEndpoint)
            }
        }


        // if there is a successful put, create an individual including the following for each endpoint
        // successful PUT request which creates the resource and the user
        // successful DELETE request with the same resource and same user
        // successful PUT request which creates the resource and the user
        // DELETE request with a different user, which should fail
        // PUT request with a different user, if it succeeds, that's a security issue
        if (individualContainingSuccessfulPut != null) {

            val listOfActions :List<RestCallAction> = individualContainingSuccessfulPut.individual.seeMainExecutableActions()


                // do this for each endpoint
            for (currentEnd : String? in allEndpoints) {

                val newList = mutableListOf<RestCallAction>()

                // for every action before the successfulPutRequest index, add a copy of the action to newList
                for(i in 0..putIndexInEndpoint ) {
                    newList.add((listOfActions[i].copy() as RestCallAction))
                }

                val currentPutAction = listOfActions[putIndexInEndpoint]

                // add a successful delete request
                val deleteReqForPut = createCopyOfActionWithDifferentVerbOrUser("action1", currentPutAction,
                    HttpVerb.DELETE, currentPutAction.auth)

                newList.add(deleteReqForPut)

                // add a successful PUT request again
                val secondPutRequest = createCopyOfActionWithDifferentVerbOrUser("action2", currentPutAction,
                    currentPutAction.verb, currentPutAction.auth)

                newList.add(secondPutRequest)

                // add a Delete request with another user
                val deleteRequestAnotherUser = createCopyOfActionWithDifferentVerbOrUser("action3" ,deleteReqForPut,
                    deleteReqForPut.verb, deleteReqForPut.auth)

                //    mutableListOf<AuthenticationHeader>()


                //    .get(0).value = headerDifferentFromPut!!

                newList.add(deleteRequestAnotherUser)

                // try PUt with another user
                val putRequestAnotherUser = createCopyOfActionWithDifferentVerbOrUser("action4", secondPutRequest,
                    secondPutRequest.verb, secondPutRequest.auth)


                newList.add(putRequestAnotherUser)

                // new individual
                val newIndividual = RestIndividual(newList, SampleType.PREDEFINED)

                // results for the new individual
                val results: MutableList<RestCallResult> = mutableListOf()

                // add results of all calls until PUT

                val result1 = RestCallResult()
                result1.setStatusCode(200)

                val result2 = RestCallResult()
                result2.setStatusCode(200)

                val result3 = RestCallResult()
                result3.setStatusCode(200)

                val result4 = RestCallResult()
                result4.setStatusCode(401)

                results.add(result1)
                results.add(result2)
                results.add(result3)
                results.add(result4)

                val fv = FitnessValue(newIndividual.size().toDouble())


                val evalIndividual : EvaluatedIndividual<RestIndividual> = EvaluatedIndividual(fv, newIndividual, results)

                archive.addIfNeeded(evalIndividual)

            }
        }


    }


    private fun createCopyOfActionWithDifferentVerbOrUser ( actionId : String,
                                                            act: RestCallAction,
                                                           newVerb : HttpVerb,
                                                           newUser: HttpWsAuthenticationInfo) : RestCallAction{

        val a = RestCallAction(actionId, newVerb, act.path,
            act.parameters.toMutableList(), newUser, act.saveLocation,
            act.locationId, act.produces, act.responseRefs, act.skipOracleChecks)

        // change the authentication information
        return a
    }



    private fun getPathParameter(act: RestCallAction) : String {

        val listOfPathParameters = mutableListOf<Any>()

        // find the path parameter
        this.recursiveTreeTraversalForFindingInformationForItem(act,
            "org.evomaster.core.problem.rest.param.PathParam", listOfPathParameters)

        if (listOfPathParameters.isNotEmpty()) {

            // starting from the path parameter, find the endpoint
            val pathParameterObject = listOfPathParameters[0]

            val listOfStringGenes = ArrayList<Any>()

            recursiveTreeTraversalForFindingInformationForItem(
                pathParameterObject,
                "org.evomaster.core.search.gene.string.StringGene",
                listOfStringGenes
            )

            if (listOfStringGenes.size > 0) {

                val stringGeneValue = (listOfStringGenes[0] as StringGene).value

                return stringGeneValue

            }
        }

        // if path parameter is not found, just return empty String
        return ""

    }

    /*
    This method is used to change the value of a given path parameter.
     */
    private fun changePathParameter(act: RestCallAction, newParam : String) {

        val listOfPathParams = ArrayList<Any>()

        // find the path parameter
        recursiveTreeTraversalForFindingInformationForItem(act,
            "org.evomaster.core.problem.rest.param.PathParam", listOfPathParams)

        if (listOfPathParams.size > 0 ) {

            // starting from the path parameter, find the endpoint
            val pathParameterObject = listOfPathParams[0]

            val listOfStringGenes = ArrayList<Any>()

            recursiveTreeTraversalForFindingInformationForItem(
                pathParameterObject,
                "org.evomaster.core.search.gene.string.StringGene",
                listOfStringGenes
            )

            if (listOfStringGenes.size > 0) {

                (listOfStringGenes[0] as StringGene).value = newParam

            }
        }

    }

    /*
     * This method is used to get the endpoint for a given action.
     */
    private fun getEndPointFromAction(act: RestCallAction) : String? {

        val listOfPathParams = ArrayList<Any>()

        // find the path parameter
        recursiveTreeTraversalForFindingInformationForItem(act, "org.evomaster.core.problem.rest.param.PathParam", listOfPathParams)

        if (listOfPathParams.size > 0 ) {

            // starting from the path parameter, find the endpoint
            val pathParameterObject = listOfPathParams[0]

            val listOfStringGenes = ArrayList<Any>()

            recursiveTreeTraversalForFindingInformationForItem(
                pathParameterObject,
                "org.evomaster.core.search.gene.string.StringGene",
                listOfStringGenes
            )

            if (listOfStringGenes.size > 0) {

                val stringGeneValue = (listOfStringGenes[0] as StringGene).value

                // find the child of type RestResourceCalls
                return stringGeneValue

            }
        }

        // if path parameter is not found, just return null
        return null

    }


    /*
    This method conducts a recursive tree traversal for finding objects of given types in the tree
    For each item which is of the type we are looking for, adds them into an ArrayList
    This method is used in many places.
    It does not have any return types, but it adds to the finalListOfItems.
     */
    private fun recursiveTreeTraversalForFindingInformationForItem(startingPoint: Any, typeOfItem : String, finalListOfItems: MutableList<Any> )  {

        if (startingPoint.javaClass.name.equals(typeOfItem)) {
            finalListOfItems.add(startingPoint)
        }

        // for each child, recursively call the function too
        for( child : Any in (startingPoint as StructuralElement).getViewOfChildren()) {
            recursiveTreeTraversalForFindingInformationForItem(child, typeOfItem, finalListOfItems)
        }

    }

    /*
    Function to extract actions and results based on properties. It is not used for now but may be used later.
     */
    private fun extractActionsAndResultsBasedOnProperties (
        restIndividuals : List<EvaluatedIndividual<RestIndividual>>,
        actionVerb : HttpVerb,
        authenticated : Boolean,
        statusCode : Int,
        actionList : MutableList<RestCallAction>,
        resultList : MutableList<ActionResult>
    ) {

        var actions: List<RestCallAction>
        var results: List<ActionResult>

        var currentAction : RestCallAction?
        var currentResult : ActionResult?

        for (restIndividual : EvaluatedIndividual<RestIndividual>  in restIndividuals) {

            actions = restIndividual.individual.seeMainExecutableActions()
            results = restIndividual.seeResults(actions)


            for (i in actions.indices) {

                currentAction = actions[i]
                currentResult = results[i]

                // to retrieve authenticated calls to POST
                if (currentAction.verb == actionVerb && currentAction.auth.name == "NoAuth" && !authenticated) {

                    val resultStatus = Integer.parseInt(currentResult.getResultValue("STATUS_CODE"))

                    if (resultStatus == statusCode) {
                        actionList.add(currentAction)
                        resultList.add(currentResult)
                    }
                }
            }
        }
    }

    /*
    Function to extract actions and results based on the same resource. It is not used for now but it can be
    used in the future.
     */
    private fun extractActionsAndResultsBasedOnSameResource (
        actionListPost : MutableList<RestCallAction>,
        actionListDelete : MutableList<RestCallAction>
    ) : MutableMap<RestCallAction, RestCallAction> {

        val result = mutableMapOf<RestCallAction, RestCallAction>()

        for( actionPost : RestCallAction in actionListPost) {

            for( actionDelete : RestCallAction in actionListDelete) {

                if (actionPost.path == actionDelete.path) {


                    // now check for same values
                    var actionPostStr : String = actionPost.toString()
                    var actionDeleteStr : String = actionDelete.toString()

                    actionPostStr = actionPostStr.substring(actionPostStr.indexOf(' '))

                    if (actionPostStr.contains('?')) {
                        actionPostStr = actionPostStr.substring(0,actionPostStr.indexOf('?') )
                    }

                    actionDeleteStr = actionDeleteStr.substring(actionDeleteStr.indexOf(' '))

                    if (actionDeleteStr.contains('?')) {
                        actionDeleteStr = actionDeleteStr.substring(0,actionDeleteStr.indexOf('?') )
                    }

                    if (actionPostStr == actionDeleteStr) {
                        result[actionPost] = actionDelete
                    }
                }
            }
        }
        return result
    }

    /*
    Function to get parameter values of an endpoint. It is not used for now but it can be used in the future.
     */
    private fun getPathParameterValuesOfEndpoint (action : RestCallAction): List<PathParam> {

        val pathParams  = mutableListOf<PathParam>()

        for( item: StructuralElement in action.getViewOfChildren()) {

            if (item::class == PathParam::class) {
                pathParams.add(item as PathParam)
            }

        }

        return pathParams

    }

    /*
    Function to change authentication in a given endpoint. It is not used for now but it can be useful in the future.
     */
    private fun changeAuthenticationInEndpoint(action : RestCallAction, sutInfo : SutInfoDto) {


        // choose an authentication Info randomly among authentication information in sutInfo
        var authenticationChanged = false
        var currentAuthenticationInfo : AuthenticationDto
        var currentIndex = 0

        while (!authenticationChanged) {

            currentAuthenticationInfo = sutInfo.infoForAuthentication[currentIndex]

            if (action.auth != currentAuthenticationInfo) {
                authenticationChanged = true
            }
            currentIndex += 1

        }

    }
}