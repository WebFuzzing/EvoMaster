package org.evomaster.core.search.service

class AverageCalculator {
    private var sum: Int = 0
    private var count: Int = 0

    // Add a new number to the calculator
    fun add(value: Int) {
        sum += value
        count++
    }

    // Return the average of the numbers added so far
    fun getAverage(): Double {
        if (count == 0)  {
            return Double.NaN
        }  else {
            return sum.toDouble() / count
        }
    }

    // Reset the calculator
    fun reset() {
        sum = 0
        count = 0
    }
}
