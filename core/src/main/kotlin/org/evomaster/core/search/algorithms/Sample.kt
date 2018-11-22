package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchAlgorithm

class Sample<T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.SAMPLE
    }


    override fun search(): Solution<T> {

        time.startSearch()

        while(time.shouldContinueSearch()){
            val ind = sampler.sample()
            ff.calculateCoverage(ind)?.run { archive.addIfNeeded(this) }
        }

        return archive.extractSolution()
    }
}