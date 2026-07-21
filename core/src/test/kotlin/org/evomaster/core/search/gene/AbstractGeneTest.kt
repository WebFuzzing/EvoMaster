package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach

abstract class AbstractGeneTest {

    protected val geneClasses = GeneSamplerForTests.geneClasses

    @AfterEach
    fun cleanCaches() {
        EnumGene.cleanCache()
    }

    companion object {
        /*
         * After each test class finishes its (potentially 1000+) seeds, hint the JVM to run
         * a full GC before the next class starts. Without this, on CI runners where algorithm
         * tests run faster than usual (less GC pressure), the heap can be fragmented and full
         * by the time GeneRandomizedTest starts, causing OOM even with -Xmx4096m.
         */
        @JvmStatic
        @AfterAll
        fun compactHeapAfterClass() {
            System.gc()
        }
    }

    protected fun getSample(seed: Long): List<Gene> {
        val rand = Randomness()
        rand.updateSeed(seed)

        return geneClasses
                .filter { !it.isAbstract }
                .map { GeneSamplerForTests.sample(it, rand) }
    }
}
