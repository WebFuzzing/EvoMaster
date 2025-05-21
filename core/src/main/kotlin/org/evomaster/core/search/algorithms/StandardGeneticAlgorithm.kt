package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
//Note that this class is not fully tested.
// It needs to be thoroughly verified whether this truly adheres to the intended algorithm.
/**
 * An implementation of the Standard Genetic Algorithm (Standard GA).
 *
 * This algorithm evolves a population of individuals using tournament selection,
 * crossover, and mutation, guided by fitness. It is one of the core metaheuristics
 * supported in EvoMaster.
 *
 * Each iteration (via [searchOnce]) forms a new population by:
 * 1. Selecting parent individuals using tournament selection,
 * 2. Optionally applying crossover with a probability defined by [config.xoverProbability],
 * 3. Optionally applying mutation to each offspring with a probability defined by [config.fixedRateMutation],
 * 4. Adding the offspring to the next generation.
 *
 * The process continues until the configured population size is reached or the time budget ends.
 *
 * Note:
 * - This implementation assumes that crossover and mutation are implemented by the superclass.
 * - The actual fitness evaluation is managed externally via the [WtsEvalIndividual] wrapper.
 */
open class StandardGeneticAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.StandardGA
    }

    /**
     * Performs a single generation of the genetic algorithm (one evolutionary step).
     *
     * Forms the next generation by:
     * - Selecting pairs of parents using tournament selection,
     * - Applying crossover (if enabled),
     * - Applying mutation to offspring (if enabled),
     * - Adding new offspring to the next population.
     *
     * Terminates early if the time budget is exceeded.
     */
    override fun searchOnce() {
        val n = config.populationSize

        // Generate the base of the next population (e.g., elitism or re-selection of fit individuals)
        val nextPop = formTheNextPopulation(population)

        while (nextPop.size < n) {
            val sizeBefore = nextPop.size

            // Select two parents
            val x = tournamentSelection()
            val y = tournamentSelection()

            // Crossover with configured probability
            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(x, y)
            }

            // Mutate each offspring with configured mutation rate
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(x)
            }
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(y)
            }

            // Add both offspring to the next population
            nextPop.add(x)
            nextPop.add(y)

            // Sanity check: we expect exactly 2 new individuals
            assert(nextPop.size == sizeBefore + 2)

            // Stop early if time budget is exhausted
            if (!time.shouldContinueSearch()) {
                break
            }
        }

        // Replace current population with the new one
        population.clear()
        population.addAll(nextPop)
    }
}
