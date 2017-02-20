package org.evomaster.core.search.algorithms.constant

import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Sampler

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantSampler : Sampler<ConstantIndividual>() {

    override fun sampleAtRandom(): ConstantIndividual{
        val gene = IntegerGene("value", 0, 0, 1000)
        gene.randomize(randomness, false)
        val ind = ConstantIndividual(gene)
        return ind
    }
}