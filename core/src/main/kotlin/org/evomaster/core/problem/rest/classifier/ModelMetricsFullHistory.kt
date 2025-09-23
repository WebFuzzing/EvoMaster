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

    /**
     * Accuracy = (TP + TN) / (TP + TN + FP + FN)
     */
    override fun estimateAccuracy(): Double {
        return if (totalSentRequests > 0) {
            correctPrediction.toDouble() / totalSentRequests
        } else 0.0
    }

    /**
     * Precision(400) = TP / (TP + FP)
     *
     * Of all requests predicted as 400, how many were truly 400.
     */
    override fun estimatePrecision400(): Double {
        val denom = truePositive400 + falsePositive400
        return if (denom > 0) truePositive400.toDouble() / denom else 0.0
    }

    /**
     * Recall(400) = TP / (TP + FN)
     *
     * Of all requests that were actually 400, how many were correctly predicted as 400.
     */
    override fun estimateRecall400(): Double {
        val denominator = truePositive400 + falseNegative400
        return if (denominator > 0) truePositive400.toDouble() / denominator else 0.0
    }

    /**
     * F1(400) = 2 * (Precision * Recall) / (Precision + Recall)
     *
     * Harmonic mean of precision and recall.
     */
    override fun estimateF1Score400(): Double {
        val p = estimatePrecision400()
        val r = estimateRecall400()
        return if ((p + r) > 0.0) 2 * (p * r) / (p + r) else 0.0
    }

    /**
     * MCC(400) = (TP*TN - FP*FN) /
     *            sqrt((TP+FP)(TP+FN)(TN+FP)(TN+FN))
     *
     * Matthews Correlation Coefficient, robust even with imbalanced data.
     */
    override fun estimateMCC400(): Double {
        val tp = truePositive400.toDouble()
        val tn = trueNegative400.toDouble()
        val fp = falsePositive400.toDouble()
        val fn = falseNegative400.toDouble()

        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) (tp * tn - fp * fn) / denominator else 0.0
    }

    /**
     * Unified metrics estimate packaged into a [ModelEvaluation].
     */
    override fun estimateMetrics(): ModelEvaluation {
        return ModelEvaluation(
            accuracy = estimateAccuracy(),
            precision400 = estimatePrecision400(),
            recall400 = estimateRecall400(),
            f1Score400 = estimateF1Score400(),
            mcc = estimateMCC400()
        )
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
