package org.evomaster.core.problem.rest.classifier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelMetricsWithTimeWindowTest {


    @Test
    fun estimateModelMetrics() {

        val delta = 0.0001
        val ma = ModelMetricsWithTimeWindow(10) // buffer large enough to hold all

        // 1) correct prediction: 200 vs. 200 -> TN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(1.0, ma.estimateAccuracy(), delta)
        assertEquals(0.0, ma.estimatePrecision400(), delta)
        assertEquals(0.0, ma.estimateRecall400(), delta)
        assertEquals(0.0, ma.estimateF1Score400(), delta)
        assertEquals(0.0, ma.estimateMCC400(), delta)

        // 2) correct prediction: 400 vs. 400 -> TP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 400)
        assertEquals(1.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0, ma.estimatePrecision400(), delta) // 1 TP, 0 FP
        assertEquals(1.0, ma.estimateRecall400(), delta)    // 1 TP, 0 FN
        assertEquals(1.0, ma.estimateF1Score400(), delta)   // perfect balance
        assertEquals(1.0, ma.estimateMCC400(), delta)       // perfect correlation

        // 3) incorrect prediction: 400 vs. 200 -> FP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(2.0/3.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta) // 1 TP, 1 FP
        assertEquals(1.0, ma.estimateRecall400(), delta)        // 1 TP, 0 FN
        assertEquals(2.0/3.0, ma.estimateF1Score400(), delta)
        assertEquals(0.5, ma.estimateMCC400(), 0.0001)

        // 4) incorrect prediction: 200 vs. 400 -> FN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(2.0/4.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta) // unchanged
        assertEquals(1.0/2.0, ma.estimateRecall400(), delta)    // 1 TP, 1 FN
        assertEquals(0.5, ma.estimateF1Score400(), delta)
        assertEquals(0.0, ma.estimateMCC400(), delta)           // balanced errors

        // 5) incorrect prediction: 200 vs. 400 -> FN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(2.0/5.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/2.0, ma.estimatePrecision400(), delta)
        assertEquals(1.0/3.0, ma.estimateRecall400(), delta)    // 1 TP, 2 FN
        assertEquals(0.4, ma.estimateF1Score400(), delta)
        assertEquals(-0.1667, ma.estimateMCC400(), 0.0001)

        // 6) incorrect prediction: 400 vs. 200 -> FP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(2.0/6.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/3.0, ma.estimatePrecision400(), delta) // 1 TP, 2 FP
        assertEquals(1.0/3.0, ma.estimateRecall400(), delta)    // unchanged
        assertEquals(1.0/3.0, ma.estimateF1Score400(), delta)
        assertEquals(-0.3333, ma.estimateMCC400(), 0.0001)

        // 7) correct prediction: 200 vs. 200 -> TN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(3.0/7.0, ma.estimateAccuracy(), delta)
        assertEquals(1.0/3.0, ma.estimatePrecision400(), delta) // unchanged
        assertEquals(1.0/3.0, ma.estimateRecall400(), delta)
        assertEquals(1.0/3.0, ma.estimateF1Score400(), delta)
        assertEquals(-0.1667, ma.estimateMCC400(), 0.0001)
    }
}