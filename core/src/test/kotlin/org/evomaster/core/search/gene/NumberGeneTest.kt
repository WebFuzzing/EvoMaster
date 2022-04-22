package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.math.min

class NumberGeneTest {

    val random = Randomness()

    @Test
    fun testDoubleGene(){
        val gene = DoubleGene("value", 12.9999999, -99.99, 99.99, precision = null, scale = 2)
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
        val gene = FloatGene("value", 12.9999999f, -99.99f, 99.99f, precision = null, scale = 2)
        assertEquals(0.01f, gene.getMinimalDelta())
        assertEquals(13.00f, gene.getFormattedValue())
        gene.value = 12.010003f
        assertEquals(12.01f, gene.getFormattedValue())

        gene.randomize(random, false, listOf())
        assertTrue(gene.value in -99.99..99.99)

        assertTrue(gene.isValid())
    }


    @Test
    fun testBigDecimalIntegralNumberFormat(){
        BigDecimalGene("foo", precision = 2, scale = 0).apply {
            assertEquals("99", getMaximum().toString())
            assertEquals("-99", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())

            this.value = BigDecimal("100")
            assertFalse(isValid())
        }

        BigDecimalGene("foo", precision = 2, scale = 0, maxInclusive = false).apply {
            assertEquals("98", getMaximum().toString())
            assertEquals("-99", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        BigDecimalGene("foo", precision = 2, scale = 0, minInclusive = false).apply {
            assertEquals("99", getMaximum().toString())
            assertEquals("-98", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        BigDecimalGene("foo", min= BigDecimal("42"), max = BigDecimal("42")).apply {
            assertFalse(isMutable())
        }

    }

    @Test
    fun testBigDecimalFloatingPointNumberFormat(){

        BigDecimalGene("foo", precision = 4, scale = 2).apply {
            assertEquals("99.99", getMaximum().toString())
            assertEquals("-99.99", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }


        BigDecimalGene("foo", precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals("99.98", getMaximum().toString())
            assertEquals("-99.99", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }


        BigDecimalGene("foo", precision = 4, scale = 2, minInclusive = false).apply {
            assertEquals("99.99", getMaximum().toString())
            assertEquals("-99.98", getMinimum().toString())
            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

    }


    @Test
    fun testDoubleGeneInclusive(){
        DoubleGene("value", min = 0.0,precision = 4, scale = 2, minInclusive = false).apply {
            assertEquals(99.99, getMaximum())
            assertEquals(0.01, getMinimum())
        }

        DoubleGene("value", max = 0.0,precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals(-0.01, getMaximum())
            assertEquals(-99.99, getMinimum())
        }

    }

    @Test
    fun testFloatDeltaInclusive(){
        FloatGene("value", min = 0.0f,precision = 4, scale = 2, minInclusive = false).apply {
            assertEquals(99.99f, getMaximum())
            assertEquals(0.01f, getMinimum())
        }

        FloatGene("value", max = 0.0f,precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals(-0.01f, getMaximum())
            assertEquals(-99.99f, getMinimum())
        }
    }

}