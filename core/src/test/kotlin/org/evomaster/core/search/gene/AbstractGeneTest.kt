package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness

abstract class AbstractGeneTest {

    protected val geneClasses = GeneSamplerForTests.geneClasses


    protected fun getSample(seed: Long): List<Gene> {
        val rand = Randomness()
        rand.updateSeed(seed)

        return geneClasses
                .filter { !it.isAbstract }
                .map { GeneSamplerForTests.sample(it, rand) }
    }
}
