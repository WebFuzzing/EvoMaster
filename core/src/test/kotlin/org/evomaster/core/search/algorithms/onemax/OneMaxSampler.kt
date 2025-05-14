package org.evomaster.core.search.algorithms.onemax

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.search.service.Sampler


class OneMaxSampler : Sampler<OneMaxIndividual>(){

    companion object{
        const val DEFAULT_N = 3
    }


    var n = DEFAULT_N

    override fun sampleAtRandom(): OneMaxIndividual {

        val sampled =  OneMaxIndividual(n, if(config.trackingEnabled()) this else null)
        sampled.doInitialize(randomness)
        sampled.doGlobalInitialize( searchGlobalState)

        return sampled
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {

    }
}
