package org.evomaster.core.search

import org.evomaster.core.output.Termination


class Solution<T>(
        val individuals: MutableList<EvaluatedIndividual<T>>,
        val testSuiteName: String,
        val termination: Termination = Termination.NONE
)
where T : Individual {

    val overall: FitnessValue = FitnessValue(0.0)
    var clusteringTime = 0

    init{
        individuals.forEach {
            overall.merge(it.fitness)
            overall.size += it.individual.size()
        }
    }
}