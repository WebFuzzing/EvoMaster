package org.evomaster.core.search.gene


import org.evomaster.core.search.gene.interfaces.WrapperGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class GeneRandomizedTest : AbstractGeneTest(){


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

        val mutable = sample.filter { it.isMutable() }

        mutable.forEach { root ->
            root.doInitialize(rand)
            checkInvariants(root) // all invariants should hold

            val copy = root.copy()
            checkInvariants(copy) //same for a copy

            if(root.isGloballyValid()) { //in these tests, global constraints are not handled
                if (root.isPrintable()) {
                    val x = root.getValueAsRawString()
                    val y = copy.getValueAsRawString()
                    // the copy should result in same phenotype
                    assertEquals(x, y, "Different phenotype for copy of ${root.javaClass}")
                } else {
                    assertThrows<Exception>("Should throw exception when trying to print ${root.javaClass}") {
                        root.getValueAsRawString()
                    }
                }
            }
        }

        verifyCopyValueFrom(mutable, rand)
    }

    private fun verifyCopyValueFrom(
        mutable: List<Gene>,
        rand: Randomness
    ) {
        val printable = mutable.filter { it.isGloballyValid() && it.isPrintable() }

        printable.forEach { root ->

            val x = root.copy()
            val sx = x.getValueAsRawString()

            val y = x.copy().apply { randomize(rand, true) }
            if(!y.isPrintable()){
                return@forEach
            }
            val sy = y.getValueAsRawString()

            if (sx == sy) {
                //randomization did not change phenotype... but might still change the genotype!!!
                //rare but this can happen, eg when internal genes in a collection are not printable
                //assertTrue(x.containsSameValueAs(y))
            } else {
                //they must be different. the same genotype must not lead to different phenotypes
                assertFalse(x.containsSameValueAs(y), "Different phenotype but same genotype for ${root.javaClass}")

                //with same type and constraints, even "unsafe" should always work
                val wasCopied = x.unsafeCopyValueFrom(y)
                assertTrue(wasCopied, "Failed to make unsafe copy for ${root.javaClass}")

                assertTrue(x.containsSameValueAs(y), "After successful copy, genotype must be the same for ${root.javaClass}")
            }

            val other = rand.choose(printable)
            //this should not crash, ie throw any exception
            try{
                x.copyValueFrom(other)
            }catch(e: Exception){
                throw AssertionError("Failed copy value for ${x.javaClass} (${x.getValueAsRawString()})" +
                        " from ${other.javaClass} (${other.getValueAsRawString()})",e)
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

        if(gene !is WrapperGene){
            val leaf = gene.getLeafGene() //should always return same gene
            assertEquals(gene, leaf)
        }

        //must contain same value as itself
        assertTrue(gene.containsSameValueAs(gene))

        //TODO add more invariants here
    }
}
