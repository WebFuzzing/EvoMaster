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

class GeneParentTest  : AbstractGeneTest() {



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
}