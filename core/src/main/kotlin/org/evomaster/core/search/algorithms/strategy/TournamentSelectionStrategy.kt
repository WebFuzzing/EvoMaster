package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness

class TournamentSelectionStrategy : SelectionStrategy {
    override fun <T : Individual> select(
        population: List<WtsEvalIndividual<T>>,
        tournamentSize: Int,
        randomness: Randomness,
        score: (WtsEvalIndividual<T>) -> Double
    ): WtsEvalIndividual<T> {
        val selected = randomness.choose(population, tournamentSize)
        return selected.maxByOrNull { score(it) } ?: randomness.choose(population)
    }
}


