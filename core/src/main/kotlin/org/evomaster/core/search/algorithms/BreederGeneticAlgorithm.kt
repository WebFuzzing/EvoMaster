package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual
import kotlin.math.max

/**
 * Breeder Genetic Algorithm (BGA)
 *
 * Differences vs Standard GA:
 * - Uses truncation selection to build a parents pool P'.
 * - At each step, creates two offspring from two random parents in P',
 *   then randomly selects ONE of the 2 offspring to add to the next population.
 */
class BreederGeneticAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.BreederGA
    }

    override fun searchOnce() {
        beginGeneration()
        frozenTargets = archive.notCoveredTargets()
        val n = config.populationSize

        // Elitism base for next generation
        val nextPop = formTheNextPopulation(population)

        // Build parents pool P' by truncation on current population
        val parentsPool = buildParentsPoolByTruncation(population)

        while (nextPop.size < n) {
            beginStep()
            val p1 = randomness.choose(parentsPool)
            val p2 = randomness.choose(parentsPool)

            // Work on copies
            val o1 = p1.copy()
            val o2 = p2.copy()

            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(o1, o2)
            }
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o1)
            }
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(o2)
            }

            // Randomly pick one child to carry over
            var chosen = o1
            if (!randomness.nextBoolean()) {
                chosen = o2
            }
            nextPop.add(chosen)

            if (!time.shouldContinueSearch()) {
                endStep()
                break
            }
            endStep()
        }

        population.clear()
        population.addAll(nextPop)
        endGeneration()
    }

    private fun buildParentsPoolByTruncation(pop: List<WtsEvalIndividual<T>>): List<WtsEvalIndividual<T>> {
        if (pop.isEmpty()) {
            return pop
        }

        val sorted = pop.sortedByDescending { score(it) }
        val k = max(config.breederParentsMin, (sorted.size * config.breederTruncationFraction).toInt())
        return sorted.take(k)
    }
}
