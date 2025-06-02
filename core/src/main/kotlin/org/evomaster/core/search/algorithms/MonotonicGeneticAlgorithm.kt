package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max
//TODO: Note that this class is not fully tested.
// It needs to be thoroughly verified whether this truly adheres to the intended algorithm.
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
            val p1 = tournamentSelection()
            val p2 = tournamentSelection()

            val o1 = p1.copy()
            val o2 = p2.copy()

            // Perform crossover with a given probability
            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(o1, o2)
            }

            // Apply mutation
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o1)
            }

            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o2)
            }

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
}
