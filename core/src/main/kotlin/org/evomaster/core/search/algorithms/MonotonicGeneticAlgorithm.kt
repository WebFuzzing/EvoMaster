package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max

/**
 * An implementation of the Monotonic Genetic Algorithm.
 *
 * The core idea of this algorithm is to maintain *monotonic improvement* over generations.
 * That is, offspring only replace parents if they are *at least as good* in terms of fitness.
 *
 * This is a more conservative variant compared to standard GAs, aiming to preserve
 * the quality of solutions across generations and avoid degradation in performance.
 *
 * This class builds on top of [StandardGeneticAlgorithm] but overrides
 * population update logic to enforce the monotonic condition.
 */
class MonotonicGeneticAlgorithm<T> : StandardGeneticAlgorithm<T>() where T : Individual {

    /** The current evolving population. */
    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    /**
     * Identifies this algorithm as MonotonicGA in the EvoMaster configuration.
     */
    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.MonotonicGA
    }

    /**
     * Clears and initializes the population before the search begins.
     */
    override fun setupBeforeSearch() {
        population.clear()
        initPopulation()
    }

    /**
     * Performs a single generation (iteration) of the Monotonic GA.
     *
     * Process:
     * - Repeatedly select two parents from the current population.
     * - Apply crossover and mutation to generate two offspring.
     * - Compare fitness of offspring vs. parents.
     * - Only accept the offspring if they are better (monotonic improvement).
     * - Otherwise, carry over the original parents to the next generation.
     *
     * Continues until the next population reaches the configured population size
     * or the time budget is exhausted.
     */
    override fun searchOnce() {
        val n = config.populationSize

        val nextPop: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        while (nextPop.size < n) {
            val p1 = selection()
            val p2 = selection()

            val o1: WtsEvalIndividual<T>
            val o2: WtsEvalIndividual<T>

            // Perform crossover with a given probability
            if (randomness.nextBoolean(config.xoverProbability)) {
                val offsprings = xover(p1, p2)
                o1 = offsprings.first
                o2 = offsprings.second
            } else {
                o1 = p1
                o2 = p2
            }

            // Apply mutation
            mutate(o1)
            mutate(o2)

            // Monotonic replacement rule:
            // Keep offspring only if they're better than the parents
            if (max(o1.calculateCombinedFitness(), o2.calculateCombinedFitness()) >
                max(p1.calculateCombinedFitness(), p2.calculateCombinedFitness())
            ) {
                nextPop.add(o1)
                nextPop.add(o2)
            } else {
                nextPop.add(p1)
                nextPop.add(p2)
            }

            if (!time.shouldContinueSearch()) {
                break
            }
        }

        // Replace the current population with the newly formed one
        population.clear()
        population.addAll(nextPop)
    }

    /**
     * Crossover operator for two individuals.
     * Creates two offspring by swapping elements up to a random split point.
     * The original parents are not modified.
     *
     * @param x The first parent.
     * @param y The second parent.
     * @return A pair of new offspring individuals.
     */
    private fun xover(
        x: WtsEvalIndividual<T>,
        y: WtsEvalIndividual<T>
    ): Pair<WtsEvalIndividual<T>, WtsEvalIndividual<T>> {
        val nx = x.suite.size
        val ny = y.suite.size

        val splitPoint = randomness.nextInt(Math.min(nx, ny))

        // Clone the parents before modifying them
        val offspring1 = x.copy()
        val offspring2 = y.copy()

        // Swap up to the split point
        (0..splitPoint).forEach {
            val temp = offspring1.suite[it]
            offspring1.suite[it] = offspring2.suite[it]
            offspring2.suite[it] = temp
        }

        return Pair(offspring1, offspring2)
    }
}
