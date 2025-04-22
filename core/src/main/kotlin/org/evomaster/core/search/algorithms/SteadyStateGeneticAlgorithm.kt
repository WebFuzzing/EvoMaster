package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max

/**
 * An implementation of the Steady-State Genetic Algorithm (SSGA).
 *
 * Unlike Standard GA, which replaces the entire population in each generation,
 * Steady-State GA updates the population incrementally by replacing a small number
 * of individuals at a time (typically just 1 or 2).
 *
 * This class inherits from StandardGeneticAlgorithm to reuse shared components,
 * but overrides search behavior to follow steady-state principles.
 */
class SteadyStateGeneticAlgorithm<T> : StandardGeneticAlgorithm<T>() where T : Individual {

    /** Local population maintained by the algorithm (different from full generational replacement). */
    private val population: MutableList<WtsEvalIndividual<T>> = mutableListOf()

    /**
     * Identifies this algorithm as SteadyStateGA in the EvoMaster configuration.
     */
    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.SteadyStateGA
    }

    /**
     * Executes a single iteration of the steady-state evolutionary process.
     *
     * Process:
     * - Select two parents.
     * - Apply crossover with a given probability to create two offspring.
     * - Apply mutation to the offspring.
     * - Replace the parents with the offspring only if the offspring are fitter.
     */
    override fun searchOnce() {
        // Select two parents from the population
        val p1 = selection()
        val p2 = selection()

        val o1: WtsEvalIndividual<T>
        val o2: WtsEvalIndividual<T>

        // Apply crossover (if enabled), otherwise pass parents as-is
        if (randomness.nextBoolean(config.xoverProbability)) {
            val offsprings = xover(p1, p2)
            o1 = offsprings.first
            o2 = offsprings.second
        } else {
            o1 = p1
            o2 = p2
        }

        // Apply mutation to each offspring
        mutate(o1)
        mutate(o2)

        // Only replace parents with offspring if the offspring are better
        if (max(o1.calculateCombinedFitness(), o2.calculateCombinedFitness()) >
            max(p1.calculateCombinedFitness(), p2.calculateCombinedFitness())) {

            // Replace both parents in the population
            population.remove(p1)
            population.remove(p2)
            population.add(o1)
            population.add(o2)
        }
    }

    /**
     * Performs crossover by copying and swapping suite elements between two individuals
     * up to a randomly chosen split point.
     *
     * @param x The first parent.
     * @param y The second parent.
     * @return A pair of offspring produced via crossover.
     */
    private fun xover(
        x: WtsEvalIndividual<T>,
        y: WtsEvalIndividual<T>
    ): Pair<WtsEvalIndividual<T>, WtsEvalIndividual<T>> {
        val nx = x.suite.size
        val ny = y.suite.size

        val splitPoint = randomness.nextInt(Math.min(nx, ny))

        // Create deep copies of parents to avoid modifying originals
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
