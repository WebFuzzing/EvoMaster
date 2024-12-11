package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.SearchAlgorithm
import org.evomaster.core.search.tracer.TraceableElementCopyFilter

/**
 * Random Walk (RW) Algorithm
 * In the Random Walk algorithm, the search process is performed by randomly mutating the current individual.
 * The algorithm is based on the idea of randomly walking through the search space.
 *  Any specific heuristic does not guide the algorithm, and it is not guaranteed to find the optimal solution.
 *
 */

class RandomWalkAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.RW
    }

    private var latestEvaluatedIndividual: EvaluatedIndividual<T>? = null

    override fun setupBeforeSearch() {
        // Nothing needs to be done before starting the search
    }

    override fun searchOnce() {

        if (latestEvaluatedIndividual == null) {

            val individual = sampler.sample(true)

            Lazy.assert { individual.isInitialized() && individual.searchGlobalState != null }

            ff.calculateCoverage(individual, modifiedSpec = null)?.run {
                archive.addIfNeeded(this)
                latestEvaluatedIndividual = this
            }

            return
        }

        val mutatedIndividual = getMutatator().mutate(latestEvaluatedIndividual as EvaluatedIndividual<T>)

        ff.calculateCoverage(mutatedIndividual)?.run {
            archive.addIfNeeded(this)
            latestEvaluatedIndividual = this
        }

    }

}
