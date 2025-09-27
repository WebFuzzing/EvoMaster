package org.evomaster.core.problem.rest.service.fitness

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.builder.RestIndividualSelectorUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.service.CallGraphService
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.StructuralElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.core.NewCookie


class BlackBoxRestFitness : RestFitness() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BlackBoxRestFitness::class.java)
    }

    override fun doCalculateCoverage(
        individual: RestIndividual,
        targets: Set<Int>,
        allTargets: Boolean,
        fullyCovered: Boolean,
        descriptiveIds: Boolean,
    ): EvaluatedIndividual<RestIndividual>? {

        val cookies = mutableMapOf<String, List<NewCookie>>()
        val tokens = mutableMapOf<String, String>()

        if(config.bbExperiments){
            /*
                If we have a controller, we MUST reset the SUT at each test execution.
                This is to avoid memory leaks in the dat structures used by EM.
                Eg, this was a huge problem for features-service with AdditionalInfo having a
                memory leak
             */
            rc.resetSUT()
        }

        cookies.putAll(AuthUtils.getCookies(client, getBaseUrl(), individual))
        tokens.putAll(AuthUtils.getTokens(client, getBaseUrl(), individual))

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()
        val mainActions = individual.seeMainExecutableActions()

        goingToStartExecutingNewTest()

        //run the test, one action at a time
        for (i in mainActions.indices) {

            reportActionIndex(i)
            val a = mainActions[i]
            val ok = handleRestCall(a, mainActions, actionResults, chainState, cookies, tokens, fv)
            actionResults[i].stopping = !ok

            if (!ok) {
                break
            }
        }

        analyzeResponseData(fv,individual,actionResults, listOf())

        if(config.blackBoxCleanUp) {
            handleCleanUpActions(individual, actionResults, chainState, cookies, tokens, fv)
        }

        handleFurtherFitnessFunctions(fv)

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    private fun handleCleanUpActions(
        individual: RestIndividual,
        actionResults: MutableList<ActionResult>,
        chainState: MutableMap<String, String>,
        cookies: Map<String, List<NewCookie>>,
        tokens: Map<String, String>,
        fv: FitnessValue
    ){
        val size = individual.size()
        //this is always going to be updated at each fitness evaluation
        individual.removeAllCleanUp()
        assert(individual.size() == size) // no side effects on main actions

        val toHandle = RestIndividualSelectorUtils.findActionsInIndividual(
            individual,
            actionResults,
            verb = HttpVerb.POST,
            statusGroup = StatusGroup.G_2xx
        ).plus(RestIndividualSelectorUtils.findActionsInIndividual(
            individual,
            actionResults,
            verb = HttpVerb.PUT,
            statusGroup = StatusGroup.G_2xx
        ))

        if(toHandle.isEmpty()){
            return
        }

        val mainActions = individual.seeMainExecutableActions()

        for(create in toHandle){

            val template = callGraphService.findDeleteFor(create.action as RestCallAction)
                ?: continue

            val delete = builder.createBoundActionOnPreviousCreate(template, create.action)
            delete.isCleanUp = true

            //check if already there an existing DELETE on same resource
            val index = mainActions.indexOf(create.action)
            assert(index >= 0)
            val existing = mainActions.filterIndexed { i, a ->
                i > index && a.verb == HttpVerb.DELETE
                        && a.path.isEquivalent(delete.path)
                        && CreateResourceUtils.doesResolveToSamePath(a,delete)
            }
            if(existing.isNotEmpty()){
                continue
            }
            individual.addCleanUpAction(delete)

            //make sure location is saved in the chain
            handleSaveLocation(create.action,create.result as RestCallResult,chainState)
        }
        individual.initializeCleanUpActions()
        val cleanup = individual.seeCleanUpActions()

        val all = mainActions.plus(cleanup) as List<RestCallAction>

        for(delete in cleanup){
            handleRestCall(delete as RestCallAction, all, actionResults, chainState, cookies, tokens, fv)
            val res = actionResults.first { it.sourceLocalId == delete.getLocalId() } as RestCallResult
            assert(StatusGroup.G_2xx.isInGroup(res.getStatusCode())
                    || res.getStatusCode() == 403) // we have some E2E with wrong implementations on purpose
            {"Wrong status: ${res.getStatusCode()}"}
        }
    }


    override fun getlocation5xx(status: Int, additionalInfoList: List<AdditionalInfoDto>, indexOfAction: Int, result: HttpWsCallResult, name: String): String? {
        /*
            In Black-Box testing, there is no info from the source/bytecode
         */
        return null
    }
}