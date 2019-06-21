package org.evomaster.core.output

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
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

    }
}