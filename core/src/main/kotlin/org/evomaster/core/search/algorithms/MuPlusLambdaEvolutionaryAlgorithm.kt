package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * (μ + λ) Evolutionary Algorithm adapted a Whole Test Suite representation.
 * Population P of size μ is evolved by generating λ offspring via mutation of each parent,
 * then selecting the best μ individuals from parents ∪ offspring.
 */
class MuPlusLambdaEvolutionaryAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.MuPlusLambdaEA

    override fun searchOnce() {
        beginGeneration()
        // Freeze targets for current generation
        frozenTargets = archive.notCoveredTargets()

        val mu = config.populationSize
        val lambda = maxOf(1, config.muPlusLambdaOffspringSize)

        val offspring: MutableList<WtsEvalIndividual<T>> = mutableListOf()

        // For each parent, generate λ/μ offspring by mutation (rounded up)
        val perParent = maxOf(1, lambda / maxOf(1, mu))
        for (p in population) {
            for (i in 0 until perParent) {
                beginStep()
                val o = p.copy()
                if (randomness.nextBoolean(config.fixedRateMutation)) {
                    mutate(o)
                }
                offspring.add(o)
                endStep()
            }
        }

        // Select best μ from parents ∪ offspring
        val merged = (population + offspring).sortedByDescending { score(it) }
        val next = merged.take(mu).map { it.copy() }.toMutableList()

        population.clear()
        population.addAll(next)
        endGeneration()
    }
}


