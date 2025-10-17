package org.evomaster.core.search.algorithms.strategy.suite

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness

interface SelectionStrategy {
    fun <T : Individual> select(
        population: List<WtsEvalIndividual<T>>,
        tournamentSize: Int,
        randomness: Randomness,
        score: (WtsEvalIndividual<T>) -> Double
    ): WtsEvalIndividual<T>
}



