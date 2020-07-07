package org.evomaster.core.search.matchproblem

import org.evomaster.core.search.service.Sampler

/**
 * created by manzh on 2020-06-16
 */
class PrimitiveTypeMatchSampler: Sampler<PrimitiveTypeMatchIndividual>() {

    var template : PrimitiveTypeMatchIndividual? = null

    override fun sampleAtRandom(): PrimitiveTypeMatchIndividual {
        val gene = template?.gene?.copy() ?: throw IllegalArgumentException("")
        gene.randomize(randomness, forceNewValue = true)
        return PrimitiveTypeMatchIndividual(gene)
    }
}