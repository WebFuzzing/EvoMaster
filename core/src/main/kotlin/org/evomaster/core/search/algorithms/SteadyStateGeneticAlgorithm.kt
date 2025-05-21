package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import org.evomaster.core.search.service.SearchAlgorithm
import kotlin.math.max
//Note that this class is not fully tested.
// It needs to be thoroughly verified whether this truly adheres to the intended algorithm.
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
        val p1 = tournamentSelection()
        val p2 = tournamentSelection()

        val o1 = p1.copy()
        val o2 = p2.copy()

        // Perform crossover with a given probability
        if (randomness.nextBoolean(config.xoverProbability)) {
            xover(o1, o2)
        }

        // Apply mutation to each offspring
        if (randomness.nextBoolean(config.fixedRateMutation)) {
            mutate(o1)
        }

        if (randomness.nextBoolean(config.fixedRateMutation)) {
            mutate(o2)
        }

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
}
