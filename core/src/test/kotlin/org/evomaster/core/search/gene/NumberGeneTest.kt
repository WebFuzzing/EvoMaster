package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

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
            // default
            assertEquals("0.00", value.toString())

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }


        BigDecimalGene("foo", max = BigDecimal.ZERO, precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals("-0.01", getMaximum().toString())
            assertEquals("-99.99", getMinimum().toString())
            // default
            assertEquals("-50.00", value.toString())

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }


        BigDecimalGene("foo", min = BigDecimal.ZERO, precision = 4, scale = 2, minInclusive = false).apply {
            assertEquals("99.99", getMaximum().toString())
            assertEquals("0.01", getMinimum().toString())
            // default
            assertEquals("50.00", value.toString())

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
            // default
            assertEquals(50.0, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        DoubleGene("value", max = 0.0,precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals(-0.01, getMaximum())
            assertEquals(-99.99, getMinimum())
            // default
            assertEquals(-50.0, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        DoubleGene("value").apply {
            assertEquals(Double.MAX_VALUE, getMaximum())
            assertEquals(-Double.MAX_VALUE, getMinimum())
        }

        DoubleGene("value", minInclusive = false, maxInclusive = false).apply {
            assertNotEquals(Double.MAX_VALUE, getMaximum())
            assertTrue(getMaximum() < Double.MAX_VALUE)
            assertNotEquals(-Double.MAX_VALUE, getMinimum())
            assertTrue(getMinimum() > -Double.MAX_VALUE)
            assertTrue(getMinimum() < getMaximum())
        }

    }

    @Test
    fun testFloatDeltaInclusive(){
        FloatGene("value", min = 0.0f,precision = 4, scale = 2, minInclusive = false).apply {
            assertEquals(99.99f, getMaximum())
            assertEquals(0.01f, getMinimum())
            // default
            assertEquals(50.0f, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        FloatGene("value", max = 0.0f,precision = 4, scale = 2, maxInclusive = false).apply {
            assertEquals(-0.01f, getMaximum())
            assertEquals(-99.99f, getMinimum())
            // default
            assertEquals(-50.0f, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        FloatGene("value").apply {
            assertEquals(Float.MAX_VALUE, getMaximum())
            assertEquals(-Float.MAX_VALUE, getMinimum())
        }

        FloatGene("value", minInclusive = false, maxInclusive = false).apply {
            assertNotEquals(Float.MAX_VALUE, getMaximum())
            assertTrue(Float.MAX_VALUE > getMaximum())
            assertNotEquals(-Float.MAX_VALUE, getMinimum())
            assertTrue(-Float.MAX_VALUE < getMinimum())
        }
    }

    @Test
    fun testIntegerGene(){
        IntegerGene("ig", precision = 2).apply {
            assertEquals(99, getMaximum())
            assertEquals(-99, getMinimum())
            // default
            assertEquals(0, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        IntegerGene("ig", min = 0, precision = 2, minInclusive = false).apply {
            assertEquals(99, getMaximum())
            assertEquals(1, getMinimum())
            // default
            assertEquals(50, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        IntegerGene("ig", max = 0, precision = 2, maxInclusive = false).apply {
            assertEquals(-1, getMaximum())
            assertEquals(-99, getMinimum())
            // default
            assertEquals(-50, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }
    }

    @Test
    fun testLongGene(){
        LongGene("ig", precision = 2).apply {
            assertEquals(99, getMaximum())
            assertEquals(-99, getMinimum())
            // default
            assertEquals(0, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        LongGene("ig", min = 0, precision = 2, minInclusive = false).apply {
            assertEquals(99, getMaximum())
            assertEquals(1, getMinimum())
            // default
            assertEquals(50, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        LongGene("ig", max = 0, precision = 2, maxInclusive = false).apply {
            assertEquals(-1, getMaximum())
            assertEquals(-99, getMinimum())
            // default
            assertEquals(-50, value)

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }
    }

    @Test
    fun testBigIntegerGene(){
        BigIntegerGene("ig", precision = 2).apply {
            assertEquals(99, getMaximum().toInt())
            assertEquals(-99, getMinimum().toInt())
            // default
            assertEquals(0, value.toInt())

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        BigIntegerGene("ig", min = BigInteger.ZERO, precision = 2, minInclusive = false).apply {
            assertEquals(99, getMaximum().toInt())
            assertEquals(1, getMinimum().toInt())
            // default
            assertEquals(50, value.toInt())

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }

        BigIntegerGene("ig", max = BigInteger.ZERO, precision = 2, maxInclusive = false).apply {
            assertEquals(-1, getMaximum().toInt())
            assertEquals(-99, getMinimum().toInt())
            // default
            assertEquals(-50, value.toInt())

            assertTrue(isValid())
            randomize(random, false, listOf())
            assertTrue(isValid())
        }
    }

    @Test
    fun testBigIntegerPrecision(){
        assertThrows(IllegalArgumentException::class.java, {BigIntegerGene("bigInt gene",precision=392236186)})
        val bi = BigIntegerGene("max", precision = 19)
        assertEquals(Long.MAX_VALUE.toBigInteger(), bi.max)
        assertEquals(Long.MIN_VALUE.toBigInteger(), bi.min)
    }

    @Test
    fun testBigDecimalPrecision(){

        assertThrows(IllegalArgumentException::class.java, {BigDecimalGene("invalid decimal gene",precision=309, scale = 0)})
        val bi = BigDecimalGene("invalid decimal gene", precision=308, scale =  Int.MAX_VALUE)
        assertNotNull(bi.max)
        assertNotNull(bi.min)
    }

    @Test
    fun testBigDecimalSetValueWithDouble(){
        val bdGene = BigDecimalGene(name = "bdGene", min = BigDecimal("-2190982811087044603"), max = BigDecimal("-1447602971353231867"), precision = 21, scale = 2)
        val value = -1.62352851568738509E+18
        bdGene.setValueWithDouble(value)
        assertEquals(value, bdGene.value.toDouble())
    }

    @Test
    fun testMinMaxConfiguration(){
        val floatGene = FloatGene("fg", min = 0.02f, max = 1.2f,scale = 0)
        assertEquals(1f, floatGene.min)
        assertEquals(1f, floatGene.max)



    }

    @Test
    fun testRoundMode(){
        val x = BigDecimal.valueOf(99.99).setScale(2, RoundingMode.DOWN)
        println(x.toPlainString())
    }

}