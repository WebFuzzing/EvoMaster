package org.evomaster.core.search


class Solution<T>(
        val individuals: MutableList<EvaluatedIndividual<T>>,
        val testSuiteName: String
        )
where T : Individual {

    val overall: FitnessValue = FitnessValue(0.0)


    init{
        individuals.forEach {
            overall.merge(it.fitness)
            overall.size += it.individual.size()
        }
    }
}