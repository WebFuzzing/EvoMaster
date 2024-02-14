package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import javax.annotation.PostConstruct

import org.apache.http.HttpStatus

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.enterprise.auth.AuthSettings
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.PathParam

import org.evomaster.core.search.*
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness

//FIXME this needs to be cleaned-up

/**
 * Service class used to do security testing after the search phase
 */
class SecurityRest {

    /**
     * Archive including test cases
     */
    @Inject
    private lateinit var archive: Archive<RestIndividual>

    @Inject
    private lateinit var sampler: RestSampler

    @Inject
    private lateinit var randomness: Randomness

    /**
     * All actions that can be defined from the OpenAPI schema
     */
    private lateinit var actionDefinitions : List<RestCallAction>


    /**
     * Individuals in the solution
     */
    private lateinit var individualsInSolution : List<EvaluatedIndividual<RestIndividual>>

    private lateinit var authSettings: AuthSettings

    /**
     * Function called after init.
     */
    @PostConstruct
    private fun postInit(){

        // get action definitions
        actionDefinitions = sampler.getActionDefinitions() as List<RestCallAction>

        authSettings = sampler.authentications
    }

    /**
     * Apply a set rule of generating new test cases, which will be added to the current archive.
     * Extract a new test suite(s) from the archive.
     */
    fun applySecurityPhase() : Solution<RestIndividual>{

        // extract individuals from the archive
        val archivedSolution : Solution<RestIndividual> = this.archive.extractSolution()
        individualsInSolution =  archivedSolution.individuals


        // we can see what is available from the schema, and then check if already existing a test for it in archive
        addForAccessControl()

        return archive.extractSolution()
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
        handleForbiddenDeleteButOkPutOrPatch()
        //TODO other rules
    }



    /**
     * Here we are considering this case:
     * - authenticated user A creates a resource X (status 2xx) | or possibly via SQL insertions
     * - authenticated user B gets 403 on DELETE X
     * - authenticated user B gets 200 on PUT/PATCH on X
     */
    private fun handleForbiddenDeleteButOkPutOrPatch() {

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

        // Check if at least 2 users. if not, nothing to do
        if (authSettings.size(HttpWsAuthenticationInfo::class.java) <= 1) {
            // nothing to test if there are not at least 2 users
            LoggingUtil.getInfoLogger().debug(
                "Security test handleForbiddenDeleteButOkPutOrPatch requires at least 2 authenticated users")
            return
        }

        // From schema, check all DELETE operations, in order to do that
        // obtain DELETE operations in the SUT according to the swagger
        val deleteOperations = getAllActionDefinitions(HttpVerb.DELETE)

        // for each endpoint for which there is a DELETE operation
        deleteOperations.forEach { delete ->

            // from archive, search if there is any test with a DELETE returning a 403
            val existing403 : List<EvaluatedIndividual<RestIndividual>> =
                RestIndividualSelectorUtils.getIndividualsWithActionAndStatus(individualsInSolution, HttpVerb.DELETE, delete.path, 403)

            var individualToChooseForTest : RestIndividual

            // if there is such an individual
            if (existing403.isNotEmpty()) {

                // current individual in the list of existing 403. Since the list is not empty,\
                // we can just get the first item
                val currentIndividualWith403 = existing403[0]

                val deleteAction = RestIndividualSelectorUtils.getActionIndexFromIndividual(currentIndividualWith403.individual, HttpVerb.DELETE,
                    delete.path)

                val deleteActionIndex = getActionWithIndex(currentIndividualWith403, deleteAction)

                // slice the individual in a way that delete all calls after the DELETE request
                individualToChooseForTest = sliceAllCallsInIndividualAfterAction(currentIndividualWith403, deleteActionIndex)
            } else {
                // there is not. need to create it based on successful create resources with authenticated user
                var verbUsedForCreation : HttpVerb? = null;
                // search for create resource for endpoint of DELETE using PUT
                lateinit var existingEndpointForCreation : EvaluatedIndividual<RestIndividual>

                val existingPutForEndpointOfDelete : List<EvaluatedIndividual<RestIndividual>> =
                    RestIndividualSelectorUtils.getIndividualsWithActionAndStatusGroup(individualsInSolution, HttpVerb.PUT, delete.path,
                        "2xx")

                lateinit var existingPostReqForEndpointOfDelete : List<EvaluatedIndividual<RestIndividual>>

                if (existingPutForEndpointOfDelete.isNotEmpty()) {
                    existingEndpointForCreation = existingPutForEndpointOfDelete[0]
                    verbUsedForCreation = HttpVerb.PUT
                }
                else {
                    // if there is no such, search for an existing POST
                    existingPostReqForEndpointOfDelete = RestIndividualSelectorUtils.getIndividualsWithActionAndStatusGroup(
                        individualsInSolution,
                        HttpVerb.POST, delete.path,
                        "2xx"
                    )

                    if (existingPostReqForEndpointOfDelete.isNotEmpty()) {
                        existingEndpointForCreation = existingPostReqForEndpointOfDelete[0]
                        verbUsedForCreation = HttpVerb.DELETE
                    }

                }

                // if neither POST not PUT exists for the endpoint, we need to handle that case specifically
                if (existingEndpointForCreation == null) {
                    // TODO
                    LoggingUtil.getInfoLogger().debug(
                        "The archive does not contain any successful PUT or POST requests, this case is not handled")
                    return
                }

                var actionIndexForCreation = -1

                if (verbUsedForCreation != null) {

                    // so we found an individual with a successful PUT or POST,  we will slice all calls after PUT or POST
                    actionIndexForCreation = RestIndividualSelectorUtils.getActionIndexFromIndividual(
                        existingEndpointForCreation.individual,
                        verbUsedForCreation,
                        delete.path
                    )

                }

                // create a copy of the existingEndpointForCreation
                val existingEndpointForCreationCopy = existingEndpointForCreation.copy()
                val actionForCreation = getActionWithIndex(existingEndpointForCreation, actionIndexForCreation)

                sliceAllCallsInIndividualAfterAction(existingEndpointForCreationCopy, actionForCreation)

                // add a DELETE call with another user
                individualToChooseForTest =
                    createIndividualWithAnotherActionAddedDifferentAuth(existingEndpointForCreationCopy,
                    actionForCreation, HttpVerb.DELETE )


            }

            // After having a set of requests in which the last one is a DELETE call with another user, add a PUT
            // with another user
            val deleteActionIndex = RestIndividualSelectorUtils.getActionIndexFromIndividual(individualToChooseForTest, HttpVerb.DELETE,
                delete.path)

            val deleteAction = getActionWithIndexRestIndividual(individualToChooseForTest, deleteActionIndex)

            var individualToAddToSuite = createIndividualWithAnotherActionAddedDifferentAuthRest(individualToChooseForTest,
                deleteAction, HttpVerb.PUT )

            // Then evaluate the fitness function to create evaluatedIndividual
            val fitness : FitnessFunction<RestIndividual> = RestFitness()

            val evaluatedIndividual = fitness.computeWholeAchievedCoverageForPostProcessing(individualToAddToSuite)

            // add the evaluated individual to the archive
            if (evaluatedIndividual != null) {
                archive.addIfNeeded(evaluatedIndividual)
            }
        }

    }

    private fun sliceAllCallsInIndividualAfterAction(individual: EvaluatedIndividual<RestIndividual>,
                                                     action: RestCallAction) : RestIndividual {

        // find the index of the individual
        val mainActions = individual.individual.seeMainExecutableActions()
        val actIndex = individual.individual.seeMainExecutableActions().indexOf(action)

        var actionList = mutableListOf<RestCallAction>()

        for (index in 0..mainActions.size) {
            if (index <= actIndex) {
                actionList.add(mainActions.get(index))
            }
        }

        val newIndividual = RestIndividual(actionList, SampleType.SECURITY)

        return newIndividual

    }


    /**
     * Just retrieve the action with a given index
     * Precondition: 0 <= index <= number of actions
     */
    private fun getActionWithIndex(individual: EvaluatedIndividual<RestIndividual>, actionIndex : Int) : RestCallAction {

        return getActionWithIndexRestIndividual(individual.individual, actionIndex)

    }

    private fun getActionWithIndexRestIndividual(individual: RestIndividual, actionIndex : Int) : RestCallAction {

        return individual.seeMainExecutableActions()[actionIndex]

    }

    /**
     * Creates another call using by:
     * First finding a
     */
    private fun createIndividualWithAnotherActionAddedDifferentAuth(individual: EvaluatedIndividual<RestIndividual>,
                                                    currentAction : RestCallAction,
                                                    newActionVerb : HttpVerb,
                                                    ) : RestIndividual {

        var actionList = mutableListOf<RestCallAction>()

        for (act in individual.individual.seeMainExecutableActions()) {
            actionList.add(act)
        }

        // create a new action with the authentication not used in current individual
        val authenticationOfOther = sampler.authentications.getDifferentOne(currentAction.auth.name, HttpWsAuthenticationInfo::class.java, randomness)
        var newRestCallAction :RestCallAction? = null;

        if (authenticationOfOther != null) {
            newRestCallAction = RestCallAction("newDelete", newActionVerb, currentAction.path,
                currentAction.parameters.toMutableList(), authenticationOfOther  )
        }

        if (newRestCallAction != null) {
            actionList.add(newRestCallAction)
        }


        val newIndividual = RestIndividual(actionList, SampleType.SECURITY)

        return newIndividual

    }



    private fun createIndividualWithAnotherActionAddedDifferentAuthRest(individual: RestIndividual,
                                                                    currentAction : RestCallAction,
                                                                    newActionVerb : HttpVerb,
    ) : RestIndividual {

        var actionList = mutableListOf<RestCallAction>()

        for (act in individual.seeMainExecutableActions()) {
            actionList.add(act)
        }

        // create a new action with the authentication not used in current individual
        val authenticationOfOther = sampler.authentications.getDifferentOne(currentAction.auth.name, HttpWsAuthenticationInfo::class.java, randomness)

        var newRestCallAction :RestCallAction? = null;

        if (authenticationOfOther != null) {
            newRestCallAction = RestCallAction("newDelete", newActionVerb, currentAction.path,
                currentAction.parameters.toMutableList(), authenticationOfOther  )
        }

        if (newRestCallAction != null) {
            actionList.add(newRestCallAction)
        }


        val newIndividual = RestIndividual(actionList, SampleType.SECURITY)

        return newIndividual

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





    /**
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


    private fun getAllActionDefinitions(verb: HttpVerb): List<RestCallAction> {
        return actionDefinitions.filter { it.verb == verb }
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