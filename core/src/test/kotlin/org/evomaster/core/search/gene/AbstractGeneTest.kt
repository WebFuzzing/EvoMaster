package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

abstract class AbstractGeneTest {

    private val genes = GeneSamplerForTests.geneClasses


    protected fun getSample(seed: Long): List<Gene> {
        val rand = Randomness()
        rand.updateSeed(seed)

        return genes
                .filter { !it.isAbstract }
                .map { GeneSamplerForTests.sample(it, rand) }
    }
}