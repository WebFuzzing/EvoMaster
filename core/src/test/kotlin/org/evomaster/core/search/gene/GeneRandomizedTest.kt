package org.evomaster.core.search.gene


import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class GeneRandomizedTest : AbstractGeneTest(){


   // @Disabled("seed 1796 is still failing")
    @TestFactory
    fun testRandomized(): Collection<DynamicTest> {
        return (1000..2000L).map {
            DynamicTest.dynamicTest("Seed: $it") { checkRandomized(it)}
        }.toList()
    }

    private fun checkRandomized(seed: Long){

        val rand = Randomness()
        rand.updateSeed(seed)
        val sample = getSample(seed)

        sample.filter { it.isMutable() }
                .forEach { root ->
                    root.doInitialize(rand)
                    checkInvariants(root) // all invariants should hold

                    val copy = root.copy()
                    checkInvariants(copy) //same for a copy

                    if(root.isGloballyValid()) { //in these tests, global constraints are not handled
                        if (root.isPrintable()) {
                            val x = root.getValueAsRawString()
                            val y = copy.getValueAsRawString()
                            assertEquals(x, y) // the copy should result in same phenotype
                        } else {
                            assertThrows<Exception>("Should throw exception when trying to print ${root.javaClass}") {
                                root.getValueAsRawString()
                            }
                        }
                    }
                }
    }


    private fun checkInvariants(gene: Gene){

        val msg = "Failed invariant for ${gene.javaClass}"

        //all same initialization state
        val initialized = gene.initialized
        assertTrue(gene.flatView().all { it.initialized == initialized }, msg)

        assertEquals(1, gene.flatView().map { it.getRoot() }.toSet().size, msg)

        //all children should have this gene as parent
        gene.getViewOfChildren().all { it.parent == gene }

        //flat view gives whole tree, so cannot be more than direct children
        assertTrue(gene.getViewOfChildren().size <= gene.flatView().size)

        //must be locally valid once gene has been randomized
        assertTrue(gene.isLocallyValid(), msg)
        //all tree must be valid, regardless of impact on phenotype
        assertTrue(gene.flatView().all {
                it.isLocallyValid()
            }
        )

        //TODO add more invariants here
    }
}
