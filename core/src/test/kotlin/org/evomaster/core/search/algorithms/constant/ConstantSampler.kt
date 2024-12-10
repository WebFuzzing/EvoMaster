package org.evomaster.core.search.algorithms.constant

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Sampler

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantSampler : Sampler<ConstantIndividual>() {

    override fun sampleAtRandom(): ConstantIndividual{
        val gene = IntegerGene("value", 0, 0, 1000)
        gene.doInitialize(randomness)
        val ind = ConstantIndividual(gene)
        ind.doGlobalInitialize(searchGlobalState)
        return ind
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {

    }
}
