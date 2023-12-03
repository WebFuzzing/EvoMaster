package org.evomaster.core.problem.rest.service

import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.SampleType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EPASampler : AbstractRestSampler() {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestSampler::class.java)
    }

    override fun sampleAtRandom(): RestIndividual {
        val actions = mutableListOf<RestCallAction>()
        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())

        (0 until n).forEach {
            actions.add(sampleRandomAction(0.05) as RestCallAction)
        }
        val ind = RestIndividual(actions, SampleType.RANDOM, mutableListOf(), this, time.evaluatedIndividuals)
        ind.doGlobalInitialize(searchGlobalState)
        return ind
    }
    override fun customizeAdHocInitialIndividuals() {
        TODO("Not yet implemented")
    }

}