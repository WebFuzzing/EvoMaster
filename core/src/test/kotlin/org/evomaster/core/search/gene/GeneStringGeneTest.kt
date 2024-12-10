package org.evomaster.core.search.gene


import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class GeneStringGeneTest : AbstractGeneTest(){

    @Test
    fun testStringGene() {
        /*
            this kind of test could be moved directly into StringGeneTest, but here it is just to checkout
            if sampler is working fine
         */
        val rand = Randomness()
        rand.updateSeed(42)
        for (i in 0..100) {
            rand.updateSeed(i.toLong())
            val s = GeneSamplerForTests.sample(StringGene::class, rand)
            s.randomize(rand, true)
            assertTrue(s.isLocallyValid())
            assertTrue(s.value.length >= s.minLength)
            assertTrue(s.value.length <= s.maxLength)
        }
    }
}
