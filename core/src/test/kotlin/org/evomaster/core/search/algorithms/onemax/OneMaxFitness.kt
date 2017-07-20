package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction


class OneMaxFitness : FitnessFunction<OneMaxIndividual>() {

    override fun doCalculateCoverage(individual: OneMaxIndividual)
            : EvaluatedIndividual<OneMaxIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())

        (0 until individual.n)
                .forEach { fv.updateTarget(it, individual.getValue(it)) }

        return EvaluatedIndividual(fv, individual.copy() as OneMaxIndividual, listOf())
    }
}