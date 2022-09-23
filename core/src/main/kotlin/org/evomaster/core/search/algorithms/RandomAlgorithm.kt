package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchAlgorithm


class RandomAlgorithm <T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.RANDOM
    }


    override fun setupBeforeSearch() {
        // Nothing needs to be done before starting the search
    }

    override fun searchOnce() {
            val individual = sampler.sample(true)

            ff.calculateCoverage(individual)?.run { archive.addIfNeeded(this) }
    }

}