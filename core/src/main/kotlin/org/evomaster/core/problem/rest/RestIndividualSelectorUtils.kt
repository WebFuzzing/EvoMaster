package org.evomaster.core.problem.rest

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.service.AbstractRestSampler
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * Utility functions to select one or more REST Individual from a group, based on different criteria.
 * Or to select specific actions.
 */
object RestIndividualSelectorUtils {

    //FIXME this needs to be cleaned-up


    /**
     * Given a resource path, we want to find any individual that has a create operation for that resource
     *
     * Assume for example the resource "/users/{id}"
     *
     * We could search for a
     * PUT "/users/id"
     * that returns 201
     *
     * or a
     * POST "/users"
     * that returns something in 2xx (not necessarily 201)
     *
     * @return null if none found
     */
    fun findIndividualWithEndpointCreationForResource( individuals: List<EvaluatedIndividual<RestIndividual>>,
                                                       resourcePath: RestPath,
                                                       mustBeAuthenticated: Boolean
    ) : Pair<EvaluatedIndividual<RestIndividual>,Endpoint>?{


        // there is not. need to create it based on successful create resources with authenticated user
        lateinit var verbUsedForCreation : HttpVerb
        // search for create resource for endpoint of DELETE using PUT
        lateinit var existingEndpointForCreation : EvaluatedIndividual<RestIndividual>

        val existingPuts  = getIndividualsWithActionAndStatus(
            individuals,
            HttpVerb.PUT,
            resourcePath,
            201,
            mustBeAuthenticated // TODO with Authentication
        )


        if(existingPuts.isNotEmpty()){
            return Pair(
                existingPuts.sortedBy { it.individual.size() }[0],
                Endpoint(HttpVerb.PUT, resourcePath)
            )
        }

        FIXME

        lateinit var existingPostReqForEndpointOfDelete : List<EvaluatedIndividual<RestIndividual>>

        if (existingPutForEndpointOfDelete.isNotEmpty()) {
            existingEndpointForCreation = existingPutForEndpointOfDelete[0]
            verbUsedForCreation = HttpVerb.PUT
        }
        else {
            // if there is no such, search for an existing POST
            existingPostReqForEndpointOfDelete = RestIndividualSelectorUtils.getIndividualsWithActionAndStatusGroup(
                individualsInSolution,
                HttpVerb.POST,
                delete.path,  // FIXME might be a parent, eg POST:/users for DELETE:/users/{id} . BUT CHECK FOR PATH STRUCTURE
                "2xx"
            )

            if (existingPostReqForEndpointOfDelete.isNotEmpty()) {
                existingEndpointForCreation = existingPostReqForEndpointOfDelete[0]
                verbUsedForCreation = HttpVerb.DELETE
            }

        }


        return null
    }


    fun findActionFromIndividual(
        individual: EvaluatedIndividual<RestIndividual>,
        actionVerb: HttpVerb,
        actionStatus: Int
    ): RestCallAction? {

        individual.evaluatedMainActions().forEach { currentAct ->

            val act = currentAct.action as RestCallAction
            val res = currentAct.result as RestCallResult

            if ( (res.getStatusCode() == actionStatus) && act.verb == actionVerb)  {
                return act
            }
        }

        return null
    }

    fun old_findIndividuals(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        statusGroup: String
    ):List<EvaluatedIndividual<RestIndividual>> {

        val individualsList = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        individuals.forEach { ind ->
            val actions = ind.evaluatedMainActions()

            val successfulDeleteContained = false

            for (a in actions) {

                val act = a.action as RestCallAction
                val res = a.result as RestCallResult


                if ( (res.getStatusCode().toString().first() == statusGroup.first())
                    && act.verb == verb
                ) {
                    if (!individualsList.contains(ind)) {
                        individualsList.add(ind)
                    }
                }

            }
        }

        return individualsList
    }

    /*
        Find individuals containing a certain action and STATUS
    */
    fun getIndividualsWithActionAndStatus(
        individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusCode: Int
    ):List<EvaluatedIndividual<RestIndividual>> {

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
  This method identifies a specific action in an individual. It is used to transform an individual containing
  one action to RestCallAction
   */
    fun findActionFromIndividuals(individualList: List<EvaluatedIndividual<RestIndividual>>,
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

    fun findIndividuals(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusCode: Int
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
           getIndexOfAction(ind,verb,path,statusCode) >= 0
        }
    }

   fun getIndividualsWithActionAndStatusGroup(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusGroup: String
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction
                val r = ea.result as RestCallResult

                a.verb == verb && a.path.isEquivalent(path) &&
                        r.getStatusCode().toString().first() == statusGroup.first()
            }
        }
    }

    fun getIndividualsWithAction(
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

    fun sliceAllCallsInIndividualAfterAction(restIndividual: RestIndividual, action: RestCallAction) : RestIndividual {

        val actionIndex = getIndexOfGivenAction(restIndividual, action)

        val ind = restIndividual.copy() as RestIndividual

        val n = ind.seeMainExecutableActions().size

        for(i in n-1 downTo actionIndex+1){
            ind.removeMainExecutableAction(i)
        }

        ind.fixGeneBindingsIfNeeded()
        ind.removeLocationId()

        return ind
    }

    /**
     * Just retrieve the action with a given index
     * Precondition: 0 <= index <= number of actions
     */
    fun getActionWithIndex(individual: EvaluatedIndividual<RestIndividual>, actionIndex : Int) : RestCallAction {

        return getActionWithIndexRestIndividual(individual.individual, actionIndex)

    }

    fun getIndexOfGivenAction(individual: RestIndividual,
                              action : RestCallAction) : Int {

        return individual.seeMainExecutableActions().indexOf(action)
    }

    fun getIndexOfAction(individual: EvaluatedIndividual<RestIndividual>,
                         verb: HttpVerb,
                         path: RestPath,
                         statusCode: Int) : Int{

        val actions = individual.evaluatedMainActions()

        for(index in actions.indices){
            val a = actions[index].action as RestCallAction
            val r = actions[index].result as RestCallResult

            if(a.verb == verb && a.path.isEquivalent(path) && r.getStatusCode() == statusCode){
                return index
            }
        }

        return -1
    }


    fun getActionWithIndexRestIndividual(individual: RestIndividual, actionIndex : Int) : RestCallAction {

        return individual.seeMainExecutableActions()[actionIndex]

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




    private fun createCopyOfActionWithDifferentVerbOrUser ( actionId : String,
                                                            act: RestCallAction,
                                                            newVerb : HttpVerb,
                                                            newUser: HttpWsAuthenticationInfo
    ) : RestCallAction{

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

                //if ( firstAuth.headers[0].value == secondAuth.headers[0].value ) {
                //    notUsedInAny = false
               // }

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

    /**
     * Get all action definitions with a given verb
     */
    fun getAllActionDefinitions(actionDefinitions: List<RestCallAction>, verb: HttpVerb): List<RestCallAction> {
        return actionDefinitions.filter { it.verb == verb }
    }

    /**
     * Create another individual from the individual by adding a new action with a new verb and with a different
     * authentication.
     * @param individual - REST indovidual which is the starting point
     * @param currentAction - REST action for the new individual
     * @param newActionVerb - verb for the new action, such as GET, POST
     */
    fun createIndividualWithAnotherActionAddedDifferentAuthRest( sampler : AbstractRestSampler,
                                                                        individual: RestIndividual,
                                                                        currentAction : RestCallAction,
                                                                        newActionVerb : HttpVerb,
                                                                        randomness : Randomness) : RestIndividual {

        /*
            Create a new individual based on some existing data.
            Need to make sure we copy this existing data (to avoid different individuals re-using shared mutable
            state), as well as resetting their internal dynamic info (eg local ids) before a new individual is
            instantiated
         */

        val actionList = mutableListOf<RestCallAction>()

        for (act in individual.seeMainExecutableActions()) {
            actionList.add(act.copy() as RestCallAction)
        }

        // create a new action with the authentication not used in current individual
        val authenticationOfOther = sampler.authentications.getDifferentOne(currentAction.auth.name,
                                                                            HttpWsAuthenticationInfo::class.java,
                                                                            randomness)

        var newRestCallAction :RestCallAction? = null;
        var newPutAction : RestCallAction? = null

        if (authenticationOfOther != null) {
            newRestCallAction = RestCallAction("newDelete", newActionVerb, currentAction.path,
                currentAction.parameters.map { it.copy() as Param }.toMutableList(), authenticationOfOther  )
        }

        if (newRestCallAction != null) {
            actionList.add(newRestCallAction)

            // add put request with authentication of other, which is a security issue if it succeeds.
            //newPutAction = RestCallAction("newDelete", HttpVerb.PUT, currentAction.path,
             //   currentAction.parameters.map { it.copy() as Param }.toMutableList(), authenticationOfOther  )

            //actionList.add(newPutAction)
        }

        // add a rest call action with


        actionList.forEach { it.resetLocalIdRecursively() }

        val newIndividual =  sampler.createIndividual(SampleType.SECURITY, actionList)
        //RestIndividual(actionList, SampleType.SECURITY)
        newIndividual.ensureFlattenedStructure()



        return newIndividual

    }

}
