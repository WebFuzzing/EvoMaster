package org.evomaster.core.utils

import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.utils.NumberMutatorUtils.MAX_DOUBLE_PRECISION
import org.evomaster.core.search.gene.utils.NumberMutatorUtils.MAX_FLOAT_PRECISION
import org.evomaster.core.search.gene.utils.NumberMutatorUtils.getDecimalEpsilon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberMutatorUtilsTest {


    @Test
    fun testGetEpsilonForFloat(){

        (1 until MAX_FLOAT_PRECISION).forEach {
            val ep = "0.${"0".repeat(it-1)}1".toFloat()
            assertEquals(ep, getDecimalEpsilon(it, 0.0f))
        }
    }

    @Test
    fun testGetEpsilonForDouble(){

        (1 until MAX_DOUBLE_PRECISION).forEach {
            val ep = "0.${"0".repeat(it-1)}1".toDouble()
            assertEquals(ep, getDecimalEpsilon(it, 0.0))
        }
    }

    @Test
    fun testWithFloatGene(){

        val precision = MAX_FLOAT_PRECISION

        (1 until precision).forEach {
            val integral = precision - it
            val max = "${"9".repeat(integral)}.${"9".repeat(it)}".toFloat()
            val min = "-${"9".repeat(integral)}.${"9".repeat(it)}".toFloat()
            val ep = "0.${"0".repeat(it-1)}1".toFloat()
            val gene = FloatGene("value", precision = precision, scale = it)
            assertEquals(min, gene.getMinimum())
            assertEquals(max, gene.getMaximum())
            assertEquals(ep, gene.getMinimalDelta())
        }

    }

    @Test
    fun testWithDoubleGene(){

        val precision = MAX_DOUBLE_PRECISION

        (1 until  precision).forEach {
            val integral = precision - it
            val max = "${"9".repeat(integral)}.${"9".repeat(it)}".toDouble()
            val min = "-${"9".repeat(integral)}.${"9".repeat(it)}".toDouble()
            val ep = "0.${"0".repeat(it-1)}1".toDouble()
            val gene = DoubleGene("value", precision = precision, scale = it)
            assertEquals(min, gene.getMinimum())
            assertEquals(max, gene.getMaximum())
            assertEquals(ep, gene.getMinimalDelta())
        }

    }

    @Test
    fun testWithBigDecimalGene(){

        val precision = MAX_DOUBLE_PRECISION

        (1 until precision).forEach {
            val integral = precision - it
            val max = "${"9".repeat(integral)}.${"9".repeat(it)}".toBigDecimal()
            val min = "-${"9".repeat(integral)}.${"9".repeat(it)}".toBigDecimal()
            val ep = "0.${"0".repeat(it-1)}1".toBigDecimal()
            val gene = BigDecimalGene("value", precision = precision, scale = it)
            assertEquals(min, gene.getMinimum())
            assertEquals(max, gene.getMaximum())
            assertEquals(ep, gene.getMinimalDelta())
        }

    }

}