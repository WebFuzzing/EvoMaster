package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArrayGeneTest {

    @Test
    fun testGene() {
        val gene = ArrayGene("array", template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
    }

    @Test
    fun testGeneMinSize() {
        val gene = ArrayGene("array", minSize = 1, maxSize =10, template=BooleanGene("boolean"))
        assertEquals(0, gene.getViewOfChildren().size)
        val randomness = Randomness()
        gene.randomize(randomness, tryToForceNewValue = true)
        assertTrue(gene.getViewOfChildren().isNotEmpty())
    }

}