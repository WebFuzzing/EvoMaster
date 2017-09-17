package org.evomaster.core.search.algorithms

import org.evomaster.core.EMConfig
import org.evomaster.core.search.Individual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.SearchAlgorithm

/**
 * Many Independent Objective (MIO) Algorithm
 */
class MioAlgorithm<T> : SearchAlgorithm<T>() where T : Individual {

    override fun getType(): EMConfig.Algorithm {
        return EMConfig.Algorithm.MIO
    }


    override fun search(): Solution<T> {

        time.startSearch()

        while(time.shouldContinueSearch()){

            val randomP = apc.getProbRandomSampling()

            if(archive.isEmpty()
                    || sampler.hasSpecialInit()
                    || randomness.nextBoolean(randomP)) {

                val ind = if(sampler.hasSpecialInit()){
                    // If there is still special init set, sample from that
                    sampler.smartSample()
                } else {
                    //note this can still be a smart sample
                    sampler.sample()
                }

                ff.calculateCoverage(ind)?.run { archive.addIfNeeded(this) }

                continue
            }

            var ei = archive.sampleIndividual()

            val nMutations = apc.getNumberOfMutations()

            getMutatator().mutateAndSave(nMutations, ei, archive)
        }

        return archive.extractSolution()
    }
}