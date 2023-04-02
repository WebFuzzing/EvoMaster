package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.reflect.full.isSuperclassOf

class GeneTest {

    private val genes = GeneSamplerForTests.geneClasses

    @Test
    fun testNumberOfGenes() {
        /*
            This number should not change, unless you explicitly add/remove any gene.
            if so, update this number accordingly
         */
        assertEquals(83, genes.size)
    }

    @Test
    fun testPackage() {

        val errors = genes.map { it.qualifiedName!! }
                .filter { !it.startsWith("org.evomaster.core.search.gene") }

        if (errors.isNotEmpty()) {
            println("Wrong packages: $errors")
        }
        assertEquals(0, errors.size)
    }

    @Test
    fun testNameSuffix() {

        val errors = genes.map { it.qualifiedName!! }
                .filter { !it.endsWith("Gene") }

        if (errors.isNotEmpty()) {
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }

    @Test
    fun testHierarchy() {

        val errors = genes.filter {
            it != Gene::class && !SimpleGene::class.isSuperclassOf(it) && !CompositeGene::class.isSuperclassOf(it)
        }

        if (errors.isNotEmpty()) {
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }


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

    @Test
    fun testCanSample() {

        val errors = genes
                .filter { !it.isAbstract }
                .filter {
                    try {
                        GeneSamplerForTests.sample(it, Randomness().apply { updateSeed(42) }); false
                    } catch (e: Exception) {
                        true
                    }
                }

        if (errors.isNotEmpty()) {
            println("Cannot sample: $errors")
        }
        assertEquals(0, errors.size)
    }

    private fun getSample(seed: Long): List<Gene> {
        val rand = Randomness()
        rand.updateSeed(seed)

        return genes
                .filter { !it.isAbstract }
                .map { GeneSamplerForTests.sample(it, rand) }
    }


    @TestFactory
    fun testParent(): Collection<DynamicTest> {
        return (0..1000L).map {
            DynamicTest.dynamicTest("Seed: $it") { checkParent(it)}
        }.toList()
    }

    private fun checkParent(seed: Long) {
        val sample = getSample(seed)

        sample.forEach { root ->
            val wholeTree = root.flatView().filter { it != root }

            wholeTree.forEach { n ->
                var p = n
                while (p.parent != null) {
                    p = p.parent as Gene
                }
                assertEquals(root, p)
            }
        }
    }

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

    @ParameterizedTest
    @ValueSource(longs = [1,2,3,4,5,6,7,8,9,10])
    fun testParentWhenCopy(seed: Long) {

        val sample = getSample(seed)

        sample.forEach { root ->
            val copy = root.copy()
            assertTrue(copy != root, "Copy is the same ref: ${root.javaClass}") //TODO what is immutable root? might fail
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