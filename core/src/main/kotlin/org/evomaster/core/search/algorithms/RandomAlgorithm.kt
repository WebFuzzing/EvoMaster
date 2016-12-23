package org.evomaster.core.search.algorithms

import org.evomaster.core.search.*
import org.evomaster.core.search.service.SearchAlgorithm


class RandomAlgorithm <T> : SearchAlgorithm<T>() where T : Individual {



    override fun search(): Solution<T> {

        time.startSearch()


        while(time.shouldContinueSearch()){

            val individual = sampler.sampleAtRandom()

            archive.addIfNeeded(ff.calculateCoverage(individual))
            time.newEvaluation()
        }

        return archive.extractSolution()
    }

}