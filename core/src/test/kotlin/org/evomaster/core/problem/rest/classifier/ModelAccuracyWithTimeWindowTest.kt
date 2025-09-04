package org.evomaster.core.problem.rest.classifier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelAccuracyWithTimeWindowTest {


    @Test
    fun estimateAccuracy() {

        val delta = 0.0001
        val ma = ModelAccuracyWithTimeWindow(4)

        assertEquals(0.0, ma.estimateAccuracy(), delta)

        ma.updatePerformance(true)
        assertEquals(1.0, ma.estimateAccuracy(), delta)

        ma.updatePerformance(true)
        assertEquals(1.0, ma.estimateAccuracy(), delta)

        ma.updatePerformance(false)
        assertEquals(0.6666666666666666, ma.estimateAccuracy(), delta)

        ma.updatePerformance(false)
        assertEquals(0.50, ma.estimateAccuracy(), delta)

        ma.updatePerformance(false)
        assertEquals(0.25, ma.estimateAccuracy(), delta)

        ma.updatePerformance(false)
        assertEquals(0.00, ma.estimateAccuracy(), delta)

        ma.updatePerformance(true)
        assertEquals(0.25, ma.estimateAccuracy(), delta)
    }

}