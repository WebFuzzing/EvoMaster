package org.evomaster.core.problem.rest.classifier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelMetricsWithTimeWindowTest {


    @Test
    fun estimateModelMetrics() {

        val delta = 0.0001
        val ma = ModelMetricsWithTimeWindow(10) // buffer large enough to hold all


        // 1) correct prediction: 200 vs. 200 -> TN (True Negative)
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(1.0, ma.estimateAccuracy(), delta)
        assertEquals(0.0, ma.estimatePrecision400(), delta) // No. 400 predicted yet

        // 2) correct prediction: 400 vs. 400 -> TP (True Positive)
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 400)
        assertEquals(1.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0, ma.estimatePrecision400(), delta) // 1 TP, 0 FP

        // 3) incorrect prediction: 400 vs. 200 -> FP (False Positive)
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(2.0/3.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta) // 1 TP, 1 FP

        // 4) incorrect prediction: 200 vs. 400 -> FN (False Negative)
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(2.0/4.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta) // unchanged

        // 5) incorrect prediction: 200 vs. 400 -> FN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(2.0/5.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta) // unchanged

        // 6) incorrect prediction: 400 vs. 200 -> FP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(2.0/6.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/3.0, ma.estimatePrecision400(), delta) // 1 TP, 2 FP

        // 7) correct prediction: 200 vs. 200 -> TN (True Negative)
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(3.0/7.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/3.0, ma.estimatePrecision400(), delta) // unchanged (1 TP, 2 FP)
    }
}