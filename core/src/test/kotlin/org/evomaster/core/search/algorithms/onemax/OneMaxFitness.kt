package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.tracer.TraceableElement


class OneMaxFitness : FitnessFunction<OneMaxIndividual>() {

    override fun doCalculateCoverage(individual: OneMaxIndividual, targets: Set<Int>)
            : EvaluatedIndividual<OneMaxIndividual>? {

        val fv = FitnessValue(individual.size().toDouble())

        (0 until individual.n)
                .forEach { fv.updateTarget(it, individual.getValue(it)) }

        return EvaluatedIndividual(
                fv, individual.copy() as OneMaxIndividual,
                listOf(), config = config, trackOperator = individual.trackOperator, index = if (config.trackingEnabled()) time.evaluatedIndividuals else TraceableElement.DEFAULT_INDEX)
    }
}