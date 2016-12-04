package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.search.*


class RandomAlgorithm <T> : SearchAlgorithm<T>() where T : Individual {

    @Inject
    private lateinit var sampler : Sampler<T>

    @Inject
    private lateinit var ff : FitnessFunction<T>


    override fun search(iterations: Int): Solution<T> {

        val archive = Archive<T>()

        for(i in 1..iterations){

            val individual = sampler.sampleAtRandom()

            archive.addIfNeeded(ff.calculateCoverage(individual))
        }

        return archive.extractSolution()
    }

}