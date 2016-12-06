package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.search.*
import org.evomaster.core.search.mutator.Mutator

/**
 * Massive Independent Objective (MIO) Algorithm
 */
class MioAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    @Inject
    private lateinit var mutator : Mutator<T>


    override fun search(iterations: Int): Solution<T> {

        val randomP = 0.2 //TODO parameter, adaptive
        //TODO parameter for shrinking, adaptive
        val archive = Archive<T>(randomness)

        var counter = 0

        while(counter < iterations){

            if(archive.isEmpty() || randomness.nextBoolean(randomP)) {
                archive.addIfNeeded(ff.calculateCoverage(sampler.sampleAtRandom()))
                counter++

                continue
            }

            var ei = archive.sampleIndividual()

            val nMutations = 1 //TODO parameter
            val left = iterations - counter
            val limit = Math.min(nMutations, left)

            var evaluated = mutator.mutate(limit, ei, archive)
            counter += evaluated
        }

        return archive.extractSolution()
    }
}