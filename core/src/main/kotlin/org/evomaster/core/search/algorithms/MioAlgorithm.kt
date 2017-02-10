package org.evomaster.core.search.algorithms

import com.google.inject.Inject
import org.evomaster.core.search.*
import org.evomaster.core.search.service.Mutator
import org.evomaster.core.search.service.SearchAlgorithm

/**
 * Massive Independent Objective (MIO) Algorithm
 */
class MioAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {


    override fun search(): Solution<T> {

        time.startSearch()

        while(time.shouldContinueSearch()){

            val randomP = apc.getProbRandomSampling()

            if(archive.isEmpty() || randomness.nextBoolean(randomP)) {

                archive.addIfNeeded(ff.calculateCoverage(sampler.sampleAtRandom()))
                time.newEvaluation()

                continue
            }

            var ei = archive.sampleIndividual()

            val nMutations = apc.getNumberOfMutations()

            getMutatator().mutateAndSave(nMutations, ei, archive)
        }

        return archive.extractSolution()
    }
}