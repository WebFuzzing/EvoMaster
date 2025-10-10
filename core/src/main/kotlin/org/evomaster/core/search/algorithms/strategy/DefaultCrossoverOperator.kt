package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness
import kotlin.math.min

class DefaultCrossoverOperator : CrossoverOperator {
    override fun <T : Individual> applyCrossover(
        x: WtsEvalIndividual<T>,
        y: WtsEvalIndividual<T>,
        randomness: Randomness
    ) {
        val nx = x.suite.size
        val ny = y.suite.size
        val splitPoint = randomness.nextInt(min(nx, ny))
        (0..splitPoint).forEach {
            val k = x.suite[it]
            x.suite[it] = y.suite[it]
            y.suite[it] = k
        }
    }
}


