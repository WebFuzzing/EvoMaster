package org.evomaster.core.search.matchproblem

import org.evomaster.core.search.service.Sampler

/**
 * created by manzh on 2020-06-16
 */
class PrimitiveTypeMatchSampler: Sampler<PrimitiveTypeMatchIndividual>() {

    var template : PrimitiveTypeMatchIndividual? = null

    override fun sampleAtRandom(): PrimitiveTypeMatchIndividual {
        val action = (template?.seeAllActions()?.get(0)?.copy() ?: throw IllegalArgumentException("there is no action")) as PrimitiveTypeMatchAction
        action.doInitialize(randomness)
        return PrimitiveTypeMatchIndividual(action)
    }
}