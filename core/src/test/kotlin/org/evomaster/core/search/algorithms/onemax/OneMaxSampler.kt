package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.service.Sampler


class OneMaxSampler : Sampler<OneMaxIndividual>(){

    var n = 3

    override fun sampleAtRandom(): OneMaxIndividual {

        val sampled = OneMaxIndividual(n)
        (0 until n).forEach {
            sampled.setValue(it, randomness.choose(listOf(0.0, 0.5, 1.0)))
        }

        return sampled
    }
}