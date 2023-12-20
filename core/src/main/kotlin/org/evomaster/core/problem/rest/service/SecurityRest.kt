package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.apache.http.HttpStatus
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Sampler
import javax.annotation.PostConstruct


/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: Sampler<RestIndividual>

    @Inject
    lateinit var remoteControllerImplementation: RemoteControllerImplementation

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>


    @PostConstruct
    private fun postInit(){
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase() : Solution<RestIndividual>{

        // we can see what is available from the schema, and then check if already existing a test for it in archive

        addForAccessControl()

        return archive.extractSolution()
    }

    private fun addForAccessControl() {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongon and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */





        accessControlBasedOnRESTGuidelines()
        //accessControlBasedOnDatabaseMonitoring() //TODO
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines() {

        // quite a few rules here that can be defined

        handleForbiddenDeleteButOkPutOrPatch()
        //TODO other
    }

    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenDeleteButOkPutOrPatch() {
        //TODO("Not yet implemented")

        /*
            check if at least 2 users.
            here, need to go through archive, for all successful create resources with authenticated user.
            for each of them, do a DELETE with a new user.
            verify if get 403.
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


        //TODO call only once, and then pass as input
        val sutInfo : SutInfoDto? = remoteControllerImplementation.getSutInfo()

        // check that there are at least two users, using the method getInfoForAuthentication()
        if (sutInfo?.infoForAuthentication?.size!! <= 1) {
            // nothing to test if there are not at least 2 users

            LoggingUtil.getInfoLogger().debug("Security test handleForbiddenDeleteButOkPutOrPatch requires at" +
                    "least 2 authenticated users")
            return
        }
        else { // TODO remove else

            // find post requests with authenticated user in the archive
            val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution();

            val individualsInSolution : List<EvaluatedIndividual<RestIndividual>>  =  archivedSolution.individuals;

            val restAuthenticatedCallsWithPost  = mutableListOf<RestCallAction>();
            //TODO redundant : type declarations
            var restAuthenticatedResultsWithPost : MutableList<ActionResult> = mutableListOf<ActionResult>();

            var restAuthenticatedCallsWithPut : MutableList<RestCallAction> = mutableListOf<RestCallAction>();
            var restAuthenticatedResultsWithPut : MutableList<ActionResult> = mutableListOf<ActionResult>();

            var restAuthenticatedCallsWithDelete : MutableList<RestCallAction> = mutableListOf<RestCallAction>();
            var restAuthenticatedResultsWithDelete : MutableList<ActionResult> = mutableListOf<ActionResult>();

            // Authenticated POST methods with SC_OK as the response
            extractActionsAndResultsBasedOnProperties(individualsInSolution,
                                                      HttpVerb.POST,
                                                      true,
                                                      HttpStatus.SC_OK,
                                                      restAuthenticatedCallsWithPost,
                                                      restAuthenticatedResultsWithPost)

            // Authenticated DELETE methods with SC_OK as the response
            extractActionsAndResultsBasedOnProperties(individualsInSolution,
                HttpVerb.DELETE,
                true,
                HttpStatus.SC_OK,
                restAuthenticatedCallsWithDelete,
                restAuthenticatedResultsWithDelete)

            // Authenticated or unauthenticated PUT methods with SC_OK as the response
            extractActionsAndResultsBasedOnProperties(individualsInSolution,
                HttpVerb.PUT,
                false,
                HttpStatus.SC_OK,
                restAuthenticatedCallsWithPut,
                restAuthenticatedResultsWithPut)

            // Authenticated or unauthenticated PUT methods with SC_OK as the response
            extractActionsAndResultsBasedOnProperties(individualsInSolution,
                HttpVerb.PUT,
                true,
                HttpStatus.SC_OK,
                restAuthenticatedCallsWithPut,
                restAuthenticatedResultsWithPut)

            // find resources for which there is both POST and DELETE by the owner
            var resourcesWithPostAndDelete : MutableMap<RestCallAction, RestCallAction>? =
                extractActionsAndResultsBasedOnSameResource(restAuthenticatedCallsWithPost,
                    restAuthenticatedCallsWithDelete)

            // this provides a map for a post request as a key and the delete request for the same endpoint with the
            // same value

            // for each matching post and delete pairs
            for (keyItem : RestCallAction in resourcesWithPostAndDelete?.keys!!) {

                // take the key as it is
                var listOfActionsInIndividual : MutableList<RestCallAction> = mutableListOf<RestCallAction>();
                var listOfExpectedResultsInIndividual : MutableList<ActionResult> = mutableListOf<ActionResult>();


                listOfActionsInIndividual.add(keyItem)
                val actionIndex : Int = restAuthenticatedCallsWithPost.indexOf(keyItem)
                listOfExpectedResultsInIndividual.add(restAuthenticatedResultsWithPost.get(actionIndex))

                // change the authentication information in the value
                // result should be 403
                var deleteReq : RestCallAction? = resourcesWithPostAndDelete.get(keyItem)

                changeAuthenticationInEndpoint(deleteReq!!, sutInfo)

                // if there is a successful PUT request, change the authentication information in PUT
                // if the result is 200, it means there is a security issue


            }

            //      take the post request as it is, change the authentication information for the delete
            //      post request should succeed but the delete request should give 403 error
            //      if there is a put request for the same endpoint, check if that put request gives 200
            //      if there is not such a put request, change the post request to put with a different user
            //      and check if that request succeeds.

            // if there are such endpoints for which POST and DELETE are on the same endpoint and
            // authenticated, add test case in which POST succeeds, but DELETE should give 403 error




            // try the DELETE endpoint with a different user, which should create 403 error

            // with the user used for DELETE, try PUT, the result should not be 200
            // if PUT does not exist, create put with parameters of POST




                System.out.println("Message here");

            }




        // for each such post request, do delete for the same resource with a different user

        // verify that we get 403 in each of those.

        // for such tests, find PUT request with a different user in the archive, if it does not exist
        // create a PUT request with a different user.

        // Since the payloads and params might have constraints, use parameters for PUT from an existing
        // successful test

        // if PUT succeeds, create a new fault definition

        // add the new test to the archive with a fake testing target.

        // check the arvhice for a successful resource creation with an authenticated user

        // if so, get that resource

        // try to delete that resource with another user other than the resource owner should get 403

        // try to modify that resource with another user other than the resource owner, should get 200

        // this is to check authentication issue in the web application.

    }




    private fun extractActionsAndResultsBasedOnProperties (
        restIndividuals : List<EvaluatedIndividual<RestIndividual>>,
        actionVerb : HttpVerb,
        authenticated : Boolean,
        statusCode : Int,
        actionList : MutableList<RestCallAction>,
        resultList : MutableList<ActionResult>
    ) {

        var actions: List<RestCallAction>;
        var results: List<ActionResult>;

        var currentAction : RestCallAction? = null;
        var currentResult : ActionResult? = null;

        for (restIndividual : EvaluatedIndividual<RestIndividual>  in restIndividuals) {

            actions = restIndividual.individual.seeMainExecutableActions()
            results = restIndividual.seeResults(actions)


            for (i in actions.indices) {

                currentAction = actions[i];

                //println(currentAction.verb);

                currentResult = results[i];

                // to retrieve authenticated calls to POST
                if (currentAction.verb == actionVerb && currentAction.auth.name.equals("NoAuth") == !authenticated) {

                    val resultStatus : Int? = Integer.parseInt(currentResult.getResultValue("STATUS_CODE"))

                    if (resultStatus == statusCode) {
                        actionList.add(currentAction);
                        resultList.add(currentResult);
                    }
                }
            }
        }
    }

    private fun extractActionsAndResultsBasedOnSameResource (
        actionListPost : MutableList<RestCallAction>,
        actionListDelete : MutableList<RestCallAction>
    ) : MutableMap<RestCallAction, RestCallAction>? {

        var result : MutableMap<RestCallAction, RestCallAction> = mutableMapOf<RestCallAction, RestCallAction>();

        for( actionPost : RestCallAction in actionListPost) {

            for( actionDelete : RestCallAction in actionListDelete) {

                if (actionPost.path == actionDelete.path) {


                    // now check for same values
                    var actionPostStr : String = actionPost.toString();
                    var actionDeleteStr : String = actionDelete.toString();

                    actionPostStr = actionPostStr.substring(actionPostStr.indexOf(' '))

                    if (actionPostStr.contains('?')) {
                        actionPostStr = actionPostStr.substring(0,actionPostStr.indexOf('?') )
                    }

                    actionDeleteStr = actionDeleteStr.substring(actionDeleteStr.indexOf(' '))

                    if (actionDeleteStr.contains('?')) {
                        actionDeleteStr = actionDeleteStr.substring(0,actionDeleteStr.indexOf('?') )
                    }

                    if (actionPostStr.equals(actionDeleteStr)) {
                        result.put(actionPost, actionDelete);
                    }
                }
            }
        }
        return result;
    }

    private fun getPathParameterValuesOfEndpoint (action : RestCallAction): List<PathParam> {

        var pathParams : MutableList<PathParam> = mutableListOf<PathParam>()

        for( item: StructuralElement in action.getViewOfChildren()) {

            if (item::class == PathParam::class) {
                pathParams.add(item as PathParam)
            }

        }

        return pathParams;

    }

    private fun changeAuthenticationInEndpoint(action : RestCallAction, sutInfo : SutInfoDto) {

        // get the current authentication Info
        var authenticationInfos : MutableList<AuthenticationDto> = sutInfo.infoForAuthentication

        // choose an authentication Info randomly among authentication information in sutInfo
        var authencitationChanged :Boolean = false
        var currentAuthencitationInfo : AuthenticationDto
        var currentIndex = 0

        while (!authencitationChanged) {

            currentAuthencitationInfo = authenticationInfos.get(currentIndex)

            if (action.auth != currentAuthencitationInfo) {
                //action.auth = currentAuthencitationInfo
                authencitationChanged = true
            }
            currentIndex = currentIndex + 1

        }

        // change the authentication Info to the randomly chosen authentication info

    }



}