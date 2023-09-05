package org.evomaster.core.search.gene


import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GeneParentRandomizeTest : AbstractGeneTest(){


    @ParameterizedTest
    @ValueSource(longs = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30])
    fun testParentRandomized(seed: Long) {
        val rand = Randomness()
        rand.updateSeed(seed)
        val sample = getSample(seed)

        sample.filter { it.isMutable() }
                .forEach { root ->
                    root.randomize(rand, true)
                    assertTrue(root.isLocallyValid(), "Not valid root: ${root.javaClass}. $root")

                    val wholeTree = root.flatView().filter { it != root }

                    wholeTree.forEach { n ->
                        var p = n
                        while (p.parent != null) {
                            p = p.parent as Gene
                        }
                        assertEquals(root, p, "Gene pointing to wrong root: ${root.javaClass}")
                    }
                }
    }

}
