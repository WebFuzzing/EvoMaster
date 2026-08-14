package org.evomaster.core.search.service.minimizer

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.search.service.Sampler

class MultiActionSampler : Sampler<MultiActionIndividual>() {

    override fun sampleAtRandom(): MultiActionIndividual {
        val sampled = MultiActionIndividual(listOf(MultiActionAction(0)))
        sampled.doInitialize(randomness)
        sampled.doGlobalInitialize(searchGlobalState)
        return sampled
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        //no-op
    }
}
