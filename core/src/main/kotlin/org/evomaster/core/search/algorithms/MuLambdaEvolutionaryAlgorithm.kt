package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.Lazy
import org.evomaster.core.search.algorithms.wts.WtsEvalIndividual

/**
 * (μ, λ) Evolutionary Algorithm.
 *
 * Population P of size μ is evolved by generating exactly λ offspring via mutation
 * and selecting the best μ individuals only from the offspring set.
 */
class MuLambdaEvolutionaryAlgorithm<T> : AbstractGeneticAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm = EMConfig.Algorithm.MuLambdaEA

    override fun searchOnce() {
        beginGeneration()
        // Freeze targets for current generation
        frozenTargets = archive.notCoveredTargets()

        val mu = config.populationSize
        val lambda = config.muLambdaOffspringSize

        val offspring: MutableList<WtsEvalIndividual<T>> = mutableListOf()
        
        val perParent = lambda / mu
        for (p in population) {
            for (i in 0 until perParent) {
                beginStep()
                val o = p.copy()
                if (randomness.nextBoolean(config.fixedRateMutation)) {
                    mutate(o)
                }
                offspring.add(o)
                if (!time.shouldContinueSearch()) {
                    endStep()
                    break
                }
                endStep()
            }
            if (!time.shouldContinueSearch()) break
        }

        // Select best μ only from offspring
        val next = offspring.sortedByDescending { score(it) }
            .take(mu)
            .map { it.copy() }
            .toMutableList()

        population.clear()
        population.addAll(next)
        endGeneration()
    }
}


