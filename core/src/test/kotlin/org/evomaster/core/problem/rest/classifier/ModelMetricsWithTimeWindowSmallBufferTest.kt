package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.classifier.quantifier.ModelMetricsWithTimeWindow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelMetricsWithTimeWindowSmallBufferTest {


    @Test
    fun estimateModelMetrics() {

        val delta = 0.0001
        val ma = ModelMetricsWithTimeWindow(4) // small buffer

        // 1) correct prediction: 200 vs. 200 -> TN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(1.0, ma.estimateMetrics().accuracy, delta)
        assertEquals(0.0, ma.estimateMetrics().precision400, delta)
        assertEquals(0.0, ma.estimateMetrics().sensitivity400, delta)
        assertEquals(0.0, ma.estimateMetrics().f1Score400, delta)
        assertEquals(0.0, ma.estimateMetrics().mcc, delta)

        // 2) correct prediction: 400 vs. 400 -> TP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 400)
        assertEquals(1.0, ma.estimateMetrics().accuracy, delta)
        assertEquals(1.0, ma.estimateMetrics().precision400, delta) // 1 TP, 0 FP
        assertEquals(1.0, ma.estimateMetrics().sensitivity400, delta)    // 1 TP, 0 FN
        assertEquals(1.0, ma.estimateMetrics().f1Score400, delta)
        assertEquals(1.0, ma.estimateMetrics().mcc, delta)

        // 3) incorrect prediction: 400 vs. 200 -> FP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(2.0/3.0, ma.estimateMetrics().accuracy, delta)
        assertEquals(0.5, ma.estimateMetrics().precision400, delta) // 1 TP, 1 FP
        assertEquals(1.0, ma.estimateMetrics().sensitivity400, delta)    // 1 TP, 0 FN
        assertEquals(2.0/3.0, ma.estimateMetrics().f1Score400, delta)
        assertEquals(0.5, ma.estimateMetrics().mcc, delta)

        // 4) incorrect prediction: 200 vs. 400 -> FN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(2.0/4.0, ma.estimateMetrics().accuracy, delta)
        assertEquals(0.5, ma.estimateMetrics().precision400, delta) // unchanged
        assertEquals(0.5, ma.estimateMetrics().sensitivity400, delta)    // 1 TP, 1 FN
        assertEquals(0.5, ma.estimateMetrics().f1Score400, delta)
        assertEquals(0.0, ma.estimateMetrics().mcc, delta)

        // 5) incorrect prediction: 200 vs. 400 -> FN
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 400)
        assertEquals(1.0/4.0, ma.estimateMetrics().accuracy, delta)    // 1 correct out of the last 4
        assertEquals(0.5, ma.estimateMetrics().precision400, delta)    // 1 TP, 1 FP
        assertEquals(1.0/3.0, ma.estimateMetrics().sensitivity400, delta)   // 1 TP, 2 FN
        assertEquals(0.4, ma.estimateMetrics().f1Score400, delta)
        assertEquals(-0.5773, ma.estimateMetrics().mcc, 0.0001)

        // 6) incorrect prediction: 400 vs. 200 -> FP
        ma.updatePerformance(predictedStatusCode = 400, actualStatusCode = 200)
        assertEquals(0.0, ma.estimateMetrics().accuracy, delta)        // all 4 are wrong
        assertEquals(0.0, ma.estimateMetrics().precision400, delta)    // no TP
        assertEquals(0.0, ma.estimateMetrics().sensitivity400, delta)       // no TP
        assertEquals(0.0, ma.estimateMetrics().f1Score400, delta)
        assertEquals(-1.0, ma.estimateMetrics().mcc, delta)

        // 7) correct prediction: 200 vs. 200 -> TN (oldest FP evicted)
        ma.updatePerformance(predictedStatusCode = 200, actualStatusCode = 200)
        assertEquals(1.0/4.0, ma.estimateMetrics().accuracy, delta)    // 1 correct out of the last 4
        assertEquals(0.0, ma.estimateMetrics().precision400, delta)    // no TP
        assertEquals(0.0, ma.estimateMetrics().sensitivity400, delta)       // no TP
        assertEquals(0.0, ma.estimateMetrics().f1Score400, delta)
        assertEquals(-0.5773, ma.estimateMetrics().mcc, 0.0001)
    }
}