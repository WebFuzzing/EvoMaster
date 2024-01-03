package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.apache.http.HttpStatus
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.auth.AuthenticationInfo
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.Minimizer
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

        //  call sutInfo Once here and pass it as a parameter for all cases.
        val sutInfo : SutInfoDto? = remoteControllerImplementation.getSutInfo()


        // we can see what is available from the schema, and then check if already existing a test for it in archive



        addForAccessControl(sutInfo)

        return archive.extractSolution()
    }

    private fun addForAccessControl(sutInfo : SutInfoDto?) {

        /*
            for black-box testing, we can only rely on REST-style guidelines to infer relations between operations
            and resources.
            for white-box, we can rely on what actually accessed in databases.
            however, there can be different kinds of databases (SQL and NoSQL, like Mongon and Neo4J).
            As we might not be able to support all of them (and other resources that are not stored in databases),
            even in white-box testing we still want to consider REST-style
         */





        accessControlBasedOnRESTGuidelines(sutInfo)
        //accessControlBasedOnDatabaseMonitoring() //TODO
    }

    private fun accessControlBasedOnDatabaseMonitoring() {
        TODO("Not yet implemented")
    }

    private fun accessControlBasedOnRESTGuidelines(sutInfo : SutInfoDto?) {

        // quite a few rules here that can be defined

        handleForbiddenDeleteButOkPutOrPatch(sutInfo)
        //TODO other
    }

    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx)
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenDeleteButOkPutOrPatch(sutInfo : SutInfoDto?) {
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
        //val sutInfo : SutInfoDto? = remoteControllerImplementation.getSutInfo()

        // check that there are at least two users, using the method getInfoForAuthentication()
        if (sutInfo?.infoForAuthentication?.size!! <= 1) {
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



        // from the archive, check if any individual has DELETE with 2xx as REST call action
        val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution();

        val individualsInSolution : List<EvaluatedIndividual<RestIndividual>>  =  archivedSolution.individuals;

        // get all endpoints used in tests
        var listOfStringGenes = ArrayList<Any>()
        var individualContainingSuccessfulDelete : EvaluatedIndividual<RestIndividual>;
        var endpointForSuccessfulDelete : String? = null;
        var deleteIndexInEndpoint : Int = -1;
        var individualContainingSuccessfulPut : EvaluatedIndividual<RestIndividual>? = null;
        var endpointForSuccessfulPut : String? = null;
        var putIndexInEndpoint : Int = -1;
        var allEndpoints = mutableListOf<String?>();
        var endpointWithDifferentAuth : RestCallAction? = null;
        var actionsContainingPut : List<RestCallAction>? = null;
        var actionsContainingDelete : List<RestCallAction>? = null;

        // for each individual, get endpoints from the individual
        for (ind : EvaluatedIndividual<RestIndividual> in individualsInSolution) {

            val actions = ind.individual.seeMainExecutableActions()
            val results = ind.seeResults(actions)
            var actionIndex : Int;
            var currentActionResult : ActionResult;
            var currentResultStatusCode : String?;
            var currentEndpoint : String?

            for (act: RestCallAction in actions) {

                // index of the action in the array, needed for accessing the result
                actionIndex = actions.indexOf(act)

                currentActionResult = results.get(actionIndex)
                currentResultStatusCode = currentActionResult.getResultValue("STATUS_CODE")
                currentEndpoint = getEndPointFromAction(act)
                // check if the action had verb delete and status success
                if (act.verb == HttpVerb.DELETE && currentResultStatusCode!!.startsWith("2"))
                {
                    individualContainingSuccessfulDelete = ind
                    endpointForSuccessfulDelete = currentEndpoint
                    deleteIndexInEndpoint = actionIndex
                    actionsContainingDelete = actions
                }
                else if (act.verb == HttpVerb.PUT && (currentResultStatusCode == HttpStatus.SC_CREATED.toString())) {
                    individualContainingSuccessfulPut = ind
                    endpointForSuccessfulPut = currentEndpoint
                    putIndexInEndpoint = actionIndex
                    actionsContainingPut = actions
                }

                allEndpoints.add(currentEndpoint)
            }
        }

        //println(sutInfo.infoForAuthentication.get(0).headers.get(0).value)
        //println(sutInfo.infoForAuthentication.get(1).headers.get(0).value)

        var headerDifferentFromPut : String? = null;

        // another loop to find an endpoint with different authentication information
        for (authHead : AuthenticationDto in sutInfo.infoForAuthentication) {

            if (authHead.headers.get(0).value !=
                actionsContainingPut!!.get(putIndexInEndpoint).auth.headers.get(0).value) {
                headerDifferentFromPut = authHead.headers.get(0).value
            }
        }

        println(headerDifferentFromPut)

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

                var newList = mutableListOf<RestCallAction>();

                // for every action before the successfulPutRequest index, add a copy of the action to newList
                for(i in 0..putIndexInEndpoint ) {
                    newList.add((listOfActions.get(i).copy() as RestCallAction))
                }

                var currentPutAction = listOfActions.get(putIndexInEndpoint)

                // add a successful delete request
                var deleteReqForPut = createCopyOfActionWithDifferentVerbOrUser(currentPutAction,
                    HttpVerb.DELETE, currentPutAction.auth)

                newList.add(deleteReqForPut)

                // add a successful PUT request again
                var secondPutRequest = createCopyOfActionWithDifferentVerbOrUser(currentPutAction,
                    currentPutAction.verb, currentPutAction.auth)

                newList.add(secondPutRequest)

                // add a Delete request with another user
                var deleteRequestAnotherUser = createCopyOfActionWithDifferentVerbOrUser(deleteReqForPut,
                    deleteReqForPut.verb, deleteReqForPut.auth)
                // change the authentication header
                deleteRequestAnotherUser.auth.headers.get(0).value = headerDifferentFromPut!!

                newList.add(deleteRequestAnotherUser)

                // try PUt with another user
                var putRequestAnotherUser = createCopyOfActionWithDifferentVerbOrUser(secondPutRequest,
                    secondPutRequest.verb, secondPutRequest.auth)
                // change the authentication header
                putRequestAnotherUser.auth.headers.get(0).value = headerDifferentFromPut!!

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

                var fv : FitnessValue = FitnessValue(newIndividual.size().toDouble())


                var evalIndividual : EvaluatedIndividual<RestIndividual> = EvaluatedIndividual(fv, newIndividual, results)

                archive.addIfNeeded(evalIndividual)

            }
        }


    }

    private fun createCopyOfActionWithDifferentVerbOrUser (act: RestCallAction,
                                                           newVerb : HttpVerb,
                                                           newUser: HttpWsAuthenticationInfo) : RestCallAction{

        // new action to create from the current action
        var newAction : RestCallAction = act.copy() as RestCallAction
        // change the verb of the new action
        newAction.verb = newVerb

        // change the authentication information
        newAction.auth = newUser

        return newAction
    }


    private fun getEndPointFromAction(act: RestCallAction) : String? {

        var listOfPathParams = ArrayList<Any>()

        // find the path parameter
        recursiveTreeTraversalForFindingInformationForItem(act, "org.evomaster.core.problem.rest.param.PathParam", listOfPathParams)

        if (listOfPathParams.size > 0 ) {

            // starting from the path parameter, find the endpoint
            val pathParameterObject = listOfPathParams.get(0)

            var listOfStringGenes = ArrayList<Any>()

            recursiveTreeTraversalForFindingInformationForItem(
                pathParameterObject,
                "org.evomaster.core.search.gene.string.StringGene",
                listOfStringGenes
            )

            if (listOfStringGenes.size > 0) {

                val stringGeneValue = (listOfStringGenes.get(0) as StringGene).value

                // find the child of type RestResourceCalls
                //print(stringGeneValue)

                return stringGeneValue

            }
        }

        // if path parameter is not found, just return null
        return null

    }

    private fun getActionIndex (ind: EvaluatedIndividual<RestIndividual>, verb: String,
                                                            responseGroup: String) {

       // val actions = ind.individual.seeMainExecutableActions()

      //  for (a : RestCallAction in actions) {
      //      val results = ind.seeResults(actions)

      //      val actionIndex = actions.indexOf(a)

       //     currentActionResult = results.get(actionIndex)
       //     currentResultStatusCode = currentActionResult.getResultValue("STATUS_CODE")
       // }

       // for (res : )


       // if (ind.seeResults().size > 1) {
       //     println("More than one result")
       // }


    }

    /*
    This method conducts a recursive tree traversal for finding objects of given types in the tree
    For each item which is of the type we are looking for, adds them into an ArrayList
     */
    private fun recursiveTreeTraversalForFindingInformationForItem(startingPoint: Any, typeOfItem : String, finalListOfItems: MutableList<Any> )  {

        if (startingPoint.javaClass.name.equals(typeOfItem)) {
            finalListOfItems.add(startingPoint)
        }

        // for each children, recursively call the function too
        //for( child: Any in startingPoint.getC)
        for( child : Any in (startingPoint as StructuralElement).getViewOfChildren()) {
            recursiveTreeTraversalForFindingInformationForItem(child, typeOfItem, finalListOfItems)
        }

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