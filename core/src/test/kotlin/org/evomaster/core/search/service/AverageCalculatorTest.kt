package org.evomaster.core.search.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AverageCalculatorTest {

    @Test
    fun testAddAndGetAverageWithMultipleValues() {
        val calculator = AverageCalculator()
        calculator.add(10)
        calculator.add(20)
        calculator.add(30)
        val result = calculator.getAverage()
        assertEquals(20.0, result, 0.01)
    }

    @Test
    fun testGetAverageWithNoValuesAdded() {
        val calculator = AverageCalculator()
        val result = calculator.getAverage()
        assertTrue(result.isNaN())
    }

    @Test
    fun testAddSingleValue() {
        val calculator = AverageCalculator()
        calculator.add(15)
        val result = calculator.getAverage()
        assertEquals(15.0, result, 0.01)
    }

    @Test
    fun testReset() {
        val calculator = AverageCalculator()
        calculator.add(50)
        calculator.add(70)
        val resultBeforeRest = calculator.getAverage()
        assertEquals(60.0, resultBeforeRest, 0.01)
        calculator.reset()
        val resultAfterReset = calculator.getAverage()
        assertTrue(resultAfterReset.isNaN())
    }

    @Test
    fun testAddNegativeValues() {
        val calculator = AverageCalculator()
        calculator.add(-10)
        calculator.add(-20)
        val result = calculator.getAverage()
        assertEquals(-15.0, result, 0.01)
    }
}
