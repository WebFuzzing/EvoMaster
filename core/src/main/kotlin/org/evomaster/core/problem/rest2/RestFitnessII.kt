package org.evomaster.core.problem.rest.serviceII

import org.evomaster.clientJava.controllerApi.dto.database.execution.ReadDbDataDto
import org.evomaster.core.database.EmptySelects
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RestFitnessII : RestFitness<RestIndividualII>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestFitnessII::class.java)
    }

    override fun doCalculateCoverage(individual: RestIndividualII): EvaluatedIndividual<RestIndividualII>? {

        rc.resetSUT()

        doInitializingActions(individual)

        val fv = FitnessValue(individual.size().toDouble())

        val actionResults: MutableList<ActionResult> = mutableListOf()

        //used for things like chaining "location" paths
        val chainState = mutableMapOf<String, String>()

        val dbData = mutableListOf<ReadDbDataDto>()

        //run the test, one action at a time
        var last = 0
        for (i in 0 until individual.actions.size) {

            rc.registerNewAction(i)
            val a = individual.actions[i]

            var ok = false

            if (a is RestCallAction) {
                ok = handleRestCall(a, actionResults, chainState)
                last = i
                if(a.verb == HttpVerb.POST && actionResults.last() is RestCallResult && !listOf(200, 201).contains((actionResults.last() as RestCallResult).getStatusCode())){
                    break
                }
            } else {
                throw IllegalStateException("Cannot handle: ${a.javaClass}")
            }

            if (!ok) {
                break
            }

            if (configuration.heuristicsForSQL) {
                val extra = rc.getExtraHeuristics()
                if (extra == null) {
                    log.warn("Cannot retrieve extra heuristics")
                    return null
                }

                if (!isEmpty(extra)) {
                    //TODO handling of toMaximize
                    fv.setExtraToMinimize(i, extra.toMinimize)
                }

                extra.readDbData?.let {
                    dbData.add(it)
                }
            }
        }

        if (!dbData.isEmpty()) {
            fv.emptySelects = EmptySelects.fromDtos(dbData)
        }

        individual.removeActionsFrom(last)

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        //TODO prioritized list

        val ids = randomness.choose(archive.notCoveredTargets(), 100)

        val dto = rc.getTestResults(ids)
        if (dto == null) {
            log.warn("Cannot retrieve coverage")
            return null
        }

        dto.targets.forEach { t ->

            if (t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value, t.actionIndex)
        }

        handleResponseTargets(fv, individual.actions, actionResults)

        expandIndividual(individual, dto.additionalInfoList)

        return EvaluatedIndividual(fv, individual.copy() as RestIndividualII, actionResults)
    }
}