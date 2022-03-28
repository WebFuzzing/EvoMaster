package org.evomaster.core.problem.rest.service

import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.NewCookie


class BlackBoxRestFitness : RestFitness() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BlackBoxRestFitness::class.java)
    }

    override fun doCalculateCoverage(individual: RestIndividual, targets: Set<Int>): EvaluatedIndividual<RestIndividual>? {

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

            /*
                currently, for bb, the auth can be only configured with the driver,
                ie, bbExperiments is enabled.
                TODO might support other manner to configure auth for bb
             */
            cookies.putAll(getCookies(individual))
            tokens.putAll(getTokens(individual))
        }

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        //run the test, one action at a time
        for (i in 0 until individual.seeActions().size) {

            val a = individual.seeActions()[i]

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState, cookies, tokens)
                actionResults[i].stopping = !ok
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }
        }

        handleResponseTargets(fv, individual.seeActions(), actionResults, listOf())

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults, trackOperator = individual.trackOperator, index = time.evaluatedIndividuals, config = config)
    }

    override fun getlocation5xx(status: Int, additionalInfoList: List<AdditionalInfoDto>, indexOfAction: Int, result: HttpWsCallResult, name: String): String? {
        /*
            In Black-Box testing, there is no info from the source/bytecode
         */
        return null
    }
}