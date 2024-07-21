package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.service.SearchAlgorithm

/**
 *  Smart Sampling (SMARTS) algorithm.
 *  A variant of Random, where different smart strategies are used to construct individuals.
 *  This can be based on structures of the problem domain, or from previous fitness evaluations.
 *  However, only black-box info will be used (eg no code coverage heuristics).
 *
 *  Note that other search algorithms could use the same smart sampling techniques when needing
 *  to sample an individual (eg initializing first population in a GA)
 */
class SmartsAlgorithm <T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.SMARTS
    }


    override fun setupBeforeSearch() {
        // Nothing needs to be done before starting the search
    }

    override fun searchOnce() {
        val individual = sampler.sample(false) // NOTE: this is main difference from Random algorithm implementation

        ff.calculateCoverage(individual, modifiedSpec = null)?.run { archive.addIfNeeded(this) }
    }

}