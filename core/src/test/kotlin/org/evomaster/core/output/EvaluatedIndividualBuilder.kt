package org.evomaster.core.output

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.*

class EvaluatedIndividualBuilder {

    companion object {
        fun buildEvaluatedIndividual(dbInitialization: MutableList<DbAction>): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {
            val format = OutputFormat.JAVA_JUNIT_4

            val baseUrlOfSut = "baseUrlOfSut"

            val sampleType = SampleType.RANDOM

            val restActions = emptyList<RestCallAction>().toMutableList()

            val individual = RestIndividual(restActions, sampleType, dbInitialization)

            val fitnessVal = FitnessValue(0.0)

            val ei = EvaluatedIndividual(fitnessVal, individual, generateIndividualResults(individual))
            return Triple(format, baseUrlOfSut, ei)
        }

        fun generateIndividualResults(individual: Individual) : List<ActionResult> = individual.seeActions(ActionFilter.ALL).map {
            if (it is DbAction) DbActionResult().also { it.setInsertExecutionResult(true) }
            else ActionResult()
        }

        fun buildResourceEvaluatedIndividual(
            dbInitialization: MutableList<DbAction>,
            groups: MutableList<Pair<MutableList<DbAction>, MutableList<RestCallAction>>>,
            results: List<ActionResult> = dbInitialization.map { DbActionResult().also { it.setInsertExecutionResult(true) } }.plus(
                groups.flatMap { g->
                    g.first.map { DbActionResult().also { it.setInsertExecutionResult(true) } }.plus(g.second.map { RestCallResult().also { it.setTimedout(true) } })
                }
            ),
            format: OutputFormat = OutputFormat.JAVA_JUNIT_4
        ): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {

            val baseUrlOfSut = "baseUrlOfSut"
            val sampleType = SampleType.SMART_RESOURCE

            val calls = groups.map {
                RestResourceCalls(null, null, it.second, it.first)
            }.toMutableList()

            val individual = RestIndividual(calls, sampleType, null, dbInitialization)

            val fitnessVal = FitnessValue(0.0)

            val ei = EvaluatedIndividual(fitnessVal, individual, results)
            return Triple(format, baseUrlOfSut, ei)
        }

    }
}