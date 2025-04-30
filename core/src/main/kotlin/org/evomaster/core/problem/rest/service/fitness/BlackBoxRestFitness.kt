package org.evomaster.core.problem.rest.service.fitness

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.httpws.auth.AuthUtils
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

        //run the test, one action at a time
        for (i in mainActions.indices) {
            val a = mainActions[i]
            val ok = handleRestCall(a, mainActions, actionResults, chainState, cookies, tokens, fv)
            actionResults[i].stopping = !ok

            if (!ok) {
                break
            }
        }

        analyzeResponseData(fv,individual,actionResults, listOf())

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    override fun getlocation5xx(status: Int, additionalInfoList: List<AdditionalInfoDto>, indexOfAction: Int, result: HttpWsCallResult, name: String): String? {
        /*
            In Black-Box testing, there is no info from the source/bytecode
         */
        return null
    }
}