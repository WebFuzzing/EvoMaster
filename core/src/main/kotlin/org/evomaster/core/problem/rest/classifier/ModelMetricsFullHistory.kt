package org.evomaster.core.problem.rest.classifier

import kotlin.math.sqrt

/**
 * Tracks the full history of classifier performance metrics,
 * without any time window (cumulative statistics).
 *
 * Maintains confusion-matrix style counters for binary classification:
 * - Positive class = HTTP 400 responses
 * - Negative class = all other responses
 *
 * @property correctPrediction the number of correct predictions made by the classifier
 * @property totalSentRequests the total number of requests evaluated
 * @property truePositive400 number of times a 400 was correctly predicted (TP)
 * @property falsePositive400 number of times 400 was predicted but not actually 400 (FP)
 * @property falseNegative400 number of times 400 was the actual result but not predicted (FN)
 * @property trueNegative400 number of times not-400 was correctly predicted (TN)
 */
class ModelMetricsFullHistory : ModelMetrics {

    var correctPrediction: Int = 0
        private set
    var totalSentRequests: Int = 0
        private set

    var truePositive400: Int = 0
        private set
    var falsePositive400: Int = 0
        private set
    var falseNegative400: Int = 0
        private set
    var trueNegative400: Int = 0
        private set

    /** Model Accuracy. See [ModelMetrics.estimateAccuracy] */
    override fun estimateAccuracy(): Double {
        return if (totalSentRequests > 0) {
            correctPrediction.toDouble() / totalSentRequests
        } else 0.0
    }

    /** Precision 400. See [ModelMetrics.estimatePrecision400] */
    override fun estimatePrecision400(): Double {
        val denominator = truePositive400 + falsePositive400
        return if (denominator > 0) truePositive400.toDouble() / denominator else 0.0
    }

    /** Recall 400. See [ModelMetrics.estimateRecall400] */
    override fun estimateRecall400(): Double {
        val denominator = truePositive400 + falseNegative400
        return if (denominator > 0) truePositive400.toDouble() / denominator else 0.0
    }


    /** Matthews Correlation Coefficient (MCC). See [ModelMetrics.estimateMCC400] */
    override fun estimateMCC400(): Double {
        val tp = truePositive400.toDouble()
        val tn = trueNegative400.toDouble()
        val fp = falsePositive400.toDouble()
        val fn = falseNegative400.toDouble()

        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) (tp * tn - fp * fn) / denominator else 0.0
    }

    /**
     * Update the performance counters after a new prediction.
     *
     * @param predictedStatusCode the predicted HTTP response
     * @param actualStatusCode    the actual HTTP response
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {
        val predictionWasCorrect = predictedStatusCode == actualStatusCode
        if (predictionWasCorrect) {
            correctPrediction++
        }
        totalSentRequests++

        when {
            actualStatusCode == 400 && predictedStatusCode == 400 -> truePositive400++
            actualStatusCode == 400 && predictedStatusCode != 400 -> falseNegative400++
            actualStatusCode != 400 && predictedStatusCode == 400 -> falsePositive400++
            actualStatusCode != 400 && predictedStatusCode != 400 -> trueNegative400++
        }
    }
}
