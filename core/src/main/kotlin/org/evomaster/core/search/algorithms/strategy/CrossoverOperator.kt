package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness

interface CrossoverOperator {
    fun <T : Individual> applyCrossover(x: WtsEvalIndividual<T>, y: WtsEvalIndividual<T>, randomness: Randomness)
}


