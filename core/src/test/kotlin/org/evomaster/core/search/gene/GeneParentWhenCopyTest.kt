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

class GeneParentWhenCopyTest : AbstractGeneTest(){


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
}