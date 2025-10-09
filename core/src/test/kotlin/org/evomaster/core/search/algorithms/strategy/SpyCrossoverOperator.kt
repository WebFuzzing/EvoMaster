package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness

/**
 * Spy crossover operator recording each call pair while delegating to the
 * default crossover implementation to keep behavior realistic.
 */
class SpyCrossoverOperator : CrossoverOperator {
    val calls = mutableListOf<Pair<WtsEvalIndividual<*>, WtsEvalIndividual<*>>>()

    override fun <T : Individual> apply(
        x: WtsEvalIndividual<T>,
        y: WtsEvalIndividual<T>,
        randomness: Randomness
    ) {
        calls.add(x to y)
        DefaultCrossoverOperator().apply(x, y, randomness)
    }
}


