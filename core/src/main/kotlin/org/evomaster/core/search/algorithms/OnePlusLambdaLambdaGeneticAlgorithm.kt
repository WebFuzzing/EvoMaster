package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * 1+(λ, λ) Genetic Algorithm adapted to Whole Test Suite representation.
 * Parent population size = 1. Each generation:
 * 1) Generate λ mutants of the current parent p via mutate; pick best mutant p' (by score).
 * 2) Do λ/2 crossovers between parent p and best mutant p' to generate λ offspring; pick best offspring o*.
 * 3) If o* is better than p (by score), replace p = o*.
 */
class OnePlusLambdaLambdaGeneticAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.OnePlusLambdaLambdaGA

    override fun setupBeforeSearch() {
        population.clear()
        // initialize parent population of size 1
        population.add(sampleSuite())
    }

    override fun searchOnce() {
        beginGeneration()
        // Freeze objectives for this generation
        frozenTargets = archive.notCoveredTargets()

        val lambda = maxOf(1, config.onePlusLambdaLambdaOffspringSize)

        // Ensure that population size == 1
        Lazy.assert { population.size == 1 }
        var parent = population.first()

        // 1) λ mutants from parent → choose best mutant p'
        val mutants: MutableList<WtsEvalIndividual<T>> = mutableListOf()
        for (i in 0 until lambda) {
            beginStep()
            val m = parent.copy()
            if (randomness.nextBoolean(config.fixedRateMutation)) {
                mutate(m)
            }
            mutants.add(m)
            endStep()
        }
        val bestMutant = mutants.maxByOrNull { score(it) } ?: parent

        // 2) λ/2 crossovers between parent and bestMutant → choose best offspring
        val offspring: MutableList<WtsEvalIndividual<T>> = mutableListOf()
        for (i in 0 until (lambda / 2)) {
            beginStep()
            val c1 = parent.copy()
            val c2 = bestMutant.copy()
            if (randomness.nextBoolean(config.xoverProbability)) {
                xover(c1, c2)
            }
            offspring.add(c1)
            offspring.add(c2)
            endStep()
        }
        val bestOffspring = (offspring.maxByOrNull { score(it) } ?: parent)

        // 3) Replace parent if best offspring is better
        if (score(bestOffspring) > score(parent)) {
            parent = bestOffspring
        }

        population.clear()
        population.add(parent)
        endGeneration()
    }
}


