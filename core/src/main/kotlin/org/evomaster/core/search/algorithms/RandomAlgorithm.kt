package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.search.*


class RandomAlgorithm <T> : SearchAlgorithm<T>() where T : Individual {



    override fun search(iterations: Int): Solution<T> {

        val archive = Archive<T>(randomness)

        for(i in 1..iterations){

            val individual = sampler.sampleAtRandom()

            archive.addIfNeeded(ff.calculateCoverage(individual))
        }

        return archive.extractSolution()
    }

}