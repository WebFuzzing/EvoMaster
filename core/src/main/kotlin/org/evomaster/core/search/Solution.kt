package org.evomaster.core.search

import org.evomaster.core.search.FitnessValue


class Solution<T>(
        val overall: FitnessValue,
        val individuals: MutableList<EvaluatedIndividual<T>>)
where T : Individual {
}