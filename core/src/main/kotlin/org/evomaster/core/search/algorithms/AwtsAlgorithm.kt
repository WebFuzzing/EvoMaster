package org.evomaster.core.search.algorithms

import org.evomaster.core.search.Archive
import org.evomaster.core.search.Individual
import org.evomaster.core.search.SearchAlgorithm
import org.evomaster.core.search.Solution

/**
 * Advanced Whole Test Suite Algorithm
 */
class AwtsAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {



    override fun search(iterations: Int): Solution<T> {

        val archive = Archive<T>(randomness)

        //start from a random individual
        archive.addIfNeeded(ff.calculateCoverage(sampler.sampleAtRandom()))

        val randomP = 0.2 //TODO parameter

        for(i in 1 until iterations){

            var individual : T
            if(randomness.nextBoolean(randomP))
                individual = sampler.sampleAtRandom()
            else
                individual = archive.sampleIndividual()

            //TODO mutate


            archive.addIfNeeded(ff.calculateCoverage(individual))
        }

        return archive.extractSolution()
    }
}