package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * LIPS (Linearly Independent Path-based Search).
 * Single-objective GA that optimizes one target (branch) at a time.
 *
 * Simplified adaptation:
 * - Picks a current target from the uncovered set.
 * - Freezes scoring on that target only.
 * - Runs one GA generation to evolve the population towards the current target.
 * - Adds discovered individuals to archive; when targets are covered, picks a new target.
 */
class LIPSAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    private var currentTarget: Int? = null

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.LIPS

    override fun searchOnce() {
        beginGeneration()

        // Compute uncovered goals
        val uncovered = archive.notCoveredTargets()
        if (uncovered.isEmpty()) {
            endGeneration()
            return
        }

        // Pick target if none, or if previously covered
        if (currentTarget == null || !uncovered.contains(currentTarget)) {
            currentTarget = uncovered.last()
        }

        // Focus scoring on the single selected target
        frozenTargets = setOf(currentTarget!!)

        // Ensure population exists
        if (population.isEmpty()) {
            initPopulation()
        }

        val n = config.populationSize
        val nextPop: MutableList<WtsEvalIndividual<T>> = formTheNextPopulation(population)

        while (nextPop.size < n) {
            beginStep()

            val p1 = tournamentSelection()
            val p2 = tournamentSelection()

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

            // Keep best wrt frozen target
            val a = if (score(o1) >= score(o2)) o1 else o2
            nextPop.add(a)

            // Stop if time is up
            if (!time.shouldContinueSearch()) {
                endStep()
                break
            }
            endStep()
        }

        population.clear()
        population.addAll(nextPop)

        // If target got covered (score 1 for any in pop), refresh target next time
        val coveredNow = population.any { score(it) >= 1.0 }
        if (coveredNow) {
            currentTarget = null
        }

        endGeneration()
    }
}


