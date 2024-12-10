package org.evomaster.core.search.matchproblem

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.search.service.Sampler

/**
 * created by manzh on 2020-06-16
 */
class PrimitiveTypeMatchSampler: Sampler<PrimitiveTypeMatchIndividual>() {

    var template : PrimitiveTypeMatchIndividual? = null

    override fun sampleAtRandom(): PrimitiveTypeMatchIndividual {
        val action = (template?.seeAllActions()?.get(0)?.copy() ?: throw IllegalArgumentException("there is no action")) as PrimitiveTypeMatchAction
        val ind = PrimitiveTypeMatchIndividual(action)
        ind.doInitialize(randomness)
        ind.doGlobalInitialize(searchGlobalState)
        return ind
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {

    }
}
