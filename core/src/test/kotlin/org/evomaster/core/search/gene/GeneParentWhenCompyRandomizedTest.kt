package org.evomaster.core.search.gene


import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource


class GeneParentWhenCompyRandomizedTest  : AbstractGeneTest(){

    @ParameterizedTest
    @ValueSource(longs = [1,2,3,4,5,6,7,8,9,10])
    fun testParentWhenCopyRandomized(seed: Long) {

        val rand = Randomness()
        rand.updateSeed(seed)
        val sample = getSample(seed)

        sample.filter { it.isMutable() }
                .forEach { root ->
                    root.randomize(rand, true)
                    assertTrue(root.isLocallyValid(), "Not valid root: ${root.javaClass}")

                    val copy = root.copy()
                    assertTrue(copy != root) //TODO what is immutable root? might fail
                    val wholeTree = copy.flatView().filter { it != root }

                    wholeTree.forEach { n ->
                        var p = n
                        while (p.parent != null) {
                            p = p.parent as Gene
                        }
                        assertEquals(copy, p, "Gene pointing to wrong root: ${root.javaClass}")
                    }
                }
    }
}
