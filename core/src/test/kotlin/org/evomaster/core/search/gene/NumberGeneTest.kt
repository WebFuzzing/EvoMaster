package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NumberGeneTest {

    val random = Randomness()

    @Test
    fun testDoubleGene(){
        val gene = DoubleGene("value", 12.9999999, -99.99, 99.99, 2)
        assertEquals(0.01, gene.getMinimalDelta())
        assertEquals(13.00, gene.getFormattedValue())
        gene.value = 12.010003
        assertEquals(12.01, gene.getFormattedValue())

        gene.randomize(random, false, listOf())
        assertTrue(gene.value in -99.99..99.99)

        assertEquals(2, gene.value.toString().split(".")[1].length)
    }

    @Test
    fun testFloatDelta(){
        val gene = FloatGene("value", 12.9999999f, -99.99f, 99.99f, 2)
        assertEquals(0.01f, gene.getMinimalDelta())
        assertEquals(13.00f, gene.getFormattedValue())
        gene.value = 12.010003f
        assertEquals(12.01f, gene.getFormattedValue())

        gene.randomize(random, false, listOf())
        assertTrue(gene.value in -99.99..99.99)

        assertTrue(gene.isValid())
    }

}