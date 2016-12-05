package org.evomaster.core.search.onemax

import org.evomaster.core.search.Sampler


class OneMaxSampler : Sampler<OneMaxIndividual>(){

    override fun sampleAtRandom(): OneMaxIndividual {

        val n = 3
        val sampled = OneMaxIndividual(n)
        (0 until n).forEach {
            sampled.setValue(it, randomness.choose(listOf(0.0, 0.5, 1.0)))
        }

        return sampled
    }
}