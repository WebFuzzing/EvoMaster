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
 * of individuals at a time:
 * Only replaces selected parents with offspring if the offspring are better
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
        // Lifecycle: start generation
        beginGeneration()
        // Freeze objectives for this generation
        frozenTargets = archive.notCoveredTargets()
        // Start single steady-state step
        beginStep()
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
        if (max(score(o1), score(o2)) >
            max(score(p1), score(p2))) {

            // Replace both parents in the population
            population.remove(p1)
            population.remove(p2)
            population.add(o1)
            population.add(o2)
        }
        // End step and generation
        endStep()
        endGeneration()
    }
}
