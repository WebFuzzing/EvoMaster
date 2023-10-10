package org.evomaster.core.problem.rest.service


import com.google.inject.Inject
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * take care of calculating/collecting fitness of [RestIndividual]
 */
class RestResourceFitness : AbstractRestFitness<RestIndividual>() {


    @Inject
    private lateinit var dm: ResourceDepManageService

    @Inject
    private lateinit var rm: ResourceManageService

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestResourceFitness::class.java)
    }

    /*
        add db check in term of each abstract resource
     */
    override fun doCalculateCoverage(
        individual: RestIndividual,
        targets: Set<Int>,
        allCovered: Boolean
    ): EvaluatedIndividual<RestIndividual>? {

        rc.resetSUT()


        /*
            there might some dbaction between rest actions.
            This map is used to record the key mapping in SQL, e.g., PK, FK
         */
        val sqlIdMap = mutableMapOf<Long, Long>()
        val executedSqlActions = mutableListOf<SqlAction>()

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //whether there exist some SQL execution failure
        var failureBefore = doDbCalls(
            individual.seeInitializingActions().filterIsInstance<SqlAction>(),
            sqlIdMap,
            true,
            executedSqlActions,
            actionResults
        )

        doMongoDbCalls(individual.seeInitializingActions().filterIsInstance<MongoDbAction>(), actionResults)

        val cookies = getCookies(individual)
        val tokens = getTokens(individual)

        val fv = FitnessValue(individual.size().toDouble())

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        var indexOfAction = 0

        val allServedHttpRequests = mutableListOf<HttpExternalServiceRequest>()

        RCallsLoop@
        for (call in individual.getResourceCalls()) {

            val result = doDbCalls(
                call.seeActions(ActionFilter.ONLY_SQL) as List<SqlAction>,
                sqlIdMap,
                failureBefore,
                executedSqlActions,
                actionResults
            )
            failureBefore = failureBefore || result

            var terminated = false

            for (a in call.getViewOfChildren().filterIsInstance<EnterpriseActionGroup>()) {
                // Note: [indexOfAction] is used to register the action in RemoteController
                //  to map it to the ActionDto.

                /*
                    Always need to reset WireMock.
                    Assume there no is External Action here. Still, the SUT could make a call to an external service
                    because new code is reached (eg due mutation of some genes).
                    We do not want such new call to re-use a mocking from a previous action which were left on.
                    So 2 options: (1) always reset before calling SUT, or (2) always reset after call in which external
                    setups were used.
                 */
                externalServiceHandler.resetServedRequests()

                val externalServiceActions = a.getExternalServiceActions()

                externalServiceActions.forEach { it.resetActive() }

                externalServiceActions.filter { it.active }
                    .filterIsInstance<HttpExternalServiceAction>()
                    .forEach {
                        // TODO: Handling WireMock for ExternalServiceActions should be generalised
                        //  to facilitate other cases such as RPC and GraphQL
                        it.buildResponse()
                    }

                val restCallAction = a.getMainAction()

                //TODO handling of inputVariables
                registerNewAction(restCallAction, indexOfAction)

                val ok: Boolean

                if (restCallAction is RestCallAction) {
                    ok = handleRestCall(restCallAction, actionResults, chainState, cookies, tokens)
                    // update creation of resources regarding response status
                    val restActionResult = actionResults.filterIsInstance<RestCallResult>()[indexOfAction]
                    call.getResourceNode().confirmFailureCreationByPost(call, restCallAction, restActionResult)

                    restActionResult.stopping = !ok
                } else {
                    throw IllegalStateException("Cannot handle: ${restCallAction.javaClass}")
                }

                // get visited wiremock instances
                val requestedExternalServiceRequests = externalServiceHandler.getAllServedExternalServiceRequests()

                allServedHttpRequests.addAll(requestedExternalServiceRequests)

                if (requestedExternalServiceRequests.isNotEmpty()) {
                    fv.registerExternalServiceRequest(indexOfAction, requestedExternalServiceRequests)
                }

                val employedDefault = requestedExternalServiceRequests.map { it.wireMockSignature }.distinct().filter {
                    externalServiceActions.filterIsInstance<HttpExternalServiceAction>()
                        .none { a -> a.request.wireMockSignature == it }
                }.associate {
                    val es = externalServiceHandler.getExternalService(it)
                    es.getRemoteHostName() to es
                }
                fv.registerExternalRequestToDefaultWM(indexOfAction, employedDefault)

                externalServiceActions.filterIsInstance<HttpExternalServiceAction>()
                    .groupBy { it.request.absoluteURL }
                    .forEach { (url, actions) ->
                        // times of url has been accessed with this rest call
                        val count = requestedExternalServiceRequests.count { r-> r.absoluteURL == url}

                        actions.forEachIndexed { index, action ->
                            if (index < count) {
                                action.confirmUsed()
                            } else {
                                action.confirmNotUsed()
                            }
                        }
                    }



                if (!ok) {
                    terminated = true
                }

                if (terminated)
                    break@RCallsLoop

                indexOfAction++
            }


        }

        val allRestResults = actionResults.filterIsInstance<RestCallResult>()
        val dto = restActionResultHandling(individual, targets, allCovered, allRestResults, fv) ?: return null

        /*
            harvest actual requests once all actions are executed
         */
        harvestResponseHandler.addHttpRequests(allServedHttpRequests)

        /*
            TODO: Man shall we update the action cluster based on expanded action?
         */
        individual.seeMainExecutableActions().forEach {
            val node = rm.getResourceNodeFromCluster(it.path.toString())
            node.updateActionsWithAdditionalParams(it)
        }

        /*
         update dependency regarding executed dto
         */
        if (config.extractSqlExecutionInfo && config.probOfEnablingResourceDependencyHeuristics > 0.0)
            dm.updateResourceTables(individual, dto)

        if (actionResults.size > individual.seeActions(ActionFilter.NO_EXTERNAL_SERVICE).size)
            log.warn("Mismatch in action results: ${actionResults.size} > ${individual.seeActions(ActionFilter.NO_EXTERNAL_SERVICE).size}")

        return EvaluatedIndividual(
            fv,
            individual.copy() as RestIndividual,
            actionResults,
            config = config,
            trackOperator = individual.trackOperator,
            index = time.evaluatedIndividuals
        )

    }
}