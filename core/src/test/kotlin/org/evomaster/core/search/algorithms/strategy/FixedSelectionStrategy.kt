package org.evomaster.core.search.algorithms.strategy

import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.algorithms.strategy.suite.SelectionStrategy

/**
 * Fixed-order selection strategy for tests. Returns a predefined FIFO sequence
 * of individuals provided via [setOrder]. Useful to deterministically control
 * which parents are selected during tests.
 */
class FixedSelectionStrategy : SelectionStrategy {
    private val queue = ArrayDeque<WtsEvalIndividual<*>>()
    private var callCount: Int = 0

    fun setOrder(order: List<WtsEvalIndividual<*>>) {
        queue.clear()
        queue.addAll(order)
    }

    fun getCallCount(): Int = callCount

    @Suppress("UNCHECKED_CAST")
    override fun <T : Individual> select(
        population: List<WtsEvalIndividual<T>>,
        tournamentSize: Int,
        randomness: Randomness,
        score: (WtsEvalIndividual<T>) -> Double
    ): WtsEvalIndividual<T> {
        callCount++
        return queue.removeFirst() as WtsEvalIndividual<T>
    }
}


