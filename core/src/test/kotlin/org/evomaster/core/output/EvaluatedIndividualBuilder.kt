package org.evomaster.core.output

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue

class EvaluatedIndividualBuilder {

    companion object {
        fun buildEvaluatedIndividual(dbInitialization: MutableList<DbAction>): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {
            val format = OutputFormat.JAVA_JUNIT_4

            val baseUrlOfSut = "baseUrlOfSut"

            val sampleType = SampleType.RANDOM

            val restActions = emptyList<RestAction>().toMutableList()


            val individual = RestIndividual(restActions, sampleType, dbInitialization)

            val fitnessVal = FitnessValue(0.0)

            val results = emptyList<ActionResult>().toMutableList()

            val ei = EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
            return Triple(format, baseUrlOfSut, ei)
        }

        fun buildResourceEvaluatedIndividual(
            dbInitialization: MutableList<DbAction>,
            groups: MutableList<Pair<MutableList<DbAction>, MutableList<RestAction>>>
        ): Triple<OutputFormat, String, EvaluatedIndividual<RestIndividual>> {

            val format = OutputFormat.JAVA_JUNIT_4
            val baseUrlOfSut = "baseUrlOfSut"
            val sampleType = SampleType.SMART_RESOURCE

            val calls = groups.map {
                RestResourceCalls(null, null, it.second, it.first)
            }.toMutableList()

            val individual = RestIndividual(calls, sampleType, null, dbInitialization)

            val fitnessVal = FitnessValue(0.0)

            val results = (individual.seeRestAction().indices).map { RestCallResult().also { it.setTimedout(true) } }

            val ei = EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
            return Triple(format, baseUrlOfSut, ei)
        }

    }
}