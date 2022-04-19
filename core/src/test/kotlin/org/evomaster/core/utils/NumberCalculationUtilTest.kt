package org.evomaster.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberCalculationUtilTest {

    @Test
    fun testPrecisionFormat(){
        assertEquals(0.01, NumberCalculationUtil.valueWithPrecisionAndScale(0.0100002, 2).toDouble())
    }

    @Test
    fun testDecBoundary(){
        val range = NumberCalculationUtil.boundaryDecimal(5, 2)
        assertEquals(-999.99, range.first)
        assertEquals(999.99, range.second)
    }

    @Test
    fun testCalculateDoubleIncrement(){
        var deltaRange = NumberCalculationUtil.calculateIncrement(-0.001, 0.005)
        assertEquals(0.006, deltaRange)

        deltaRange = NumberCalculationUtil.calculateIncrement(-0.001, 0.005, 0.002)
        assertEquals(0.002, deltaRange)

        val maxRange = NumberCalculationUtil.calculateIncrement(-0.001, Double.MAX_VALUE)
        assertEquals(Long.MAX_VALUE.toDouble(), maxRange)

    }

    @Test
    fun testCalculateLongIncrement(){
        var deltaRange = NumberCalculationUtil.calculateIncrement(-1, 5, minIncrement = 0L)
        assertEquals(6, deltaRange)

        deltaRange = NumberCalculationUtil.calculateIncrement(-1, 5, maxIncrement = 2)
        assertEquals(2, deltaRange)

        val maxRange = NumberCalculationUtil.calculateIncrement(Long.MIN_VALUE, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, maxRange)

    }
}