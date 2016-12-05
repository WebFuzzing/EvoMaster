package org.evomaster.core.search


class Solution<T>(
        val overall: FitnessValue,
        val individuals: MutableList<EvaluatedIndividual<T>>)
where T : Individual {
}