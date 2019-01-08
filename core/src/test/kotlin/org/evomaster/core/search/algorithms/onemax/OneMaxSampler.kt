package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.service.Sampler


class OneMaxSampler : Sampler<OneMaxIndividual>(){

    var n = 3

    override fun sampleAtRandom(): OneMaxIndividual {

        val sampled = if(config.enableTrackIndividual) OneMaxIndividual(n, "sampleAtRandom::"+getTrackOperator()!!, mutableListOf()) else OneMaxIndividual(n,"sampleAtRandom::"+getTrackOperator()!!)
        (0 until n).forEach {
            sampled.setValue(it, randomness.choose(listOf(0.0, 0.25, 0.5, 0.75, 1.0)))
        }

        return sampled
    }

    override fun getTrackOperator(): String {
        return OneMaxSampler::class.java.simpleName
    }

}