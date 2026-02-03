package org.evomaster.core.problem.rest.classifier.quantifier

import kotlin.math.sqrt

/**
 * Interface for tracking and estimating performance metrics of a model
 * that predicts HTTP response status codes for REST calls.
 *
 * Implementations are expected to maintain internal counters for the confusion matrix:
 * - TP (True Positives): predicted 400 and actual 400
 * - TN (True Negatives): predicted not-400 and actual not-400
 * - FP (False Positives): predicted 400 but actually not-400
 * - FN (False Negatives): predicted not-400 but actual 400
 *
 * @property truePositive400 TP
 * @property falsePositive400 FP
 * @property falseNegative400 FN
 * @property trueNegative400 TN
 *
 * From these values, various metrics can be derived, such as:
 * - Accuracy
 * - Precision (for 400s)
 * - Recall (for 400s)
 * - F1 score (for 400s)
 * - Negative Predictive Value (NPV)
 * - Specificity (True Negative Rate)
 * - MCC (Matthews Correlation Coefficient)
 */
interface ModelMetrics {

    /**
     * The total number of requests evaluated by the classifier.
     * This value is crucial because the classifier avoids updating its model
     * until the total number of processed requests reaches the configured
     * warm-up threshold [org.evomaster.core.EMConfig.aiResponseClassifierWarmup].
     */
    val totalSentRequests: Int

    val truePositive400: Int
    val falsePositive400: Int
    val falseNegative400: Int
    val trueNegative400: Int

    /**
     * Total number of predictions currently held in the active window.
     *
     * For a full-history model [ModelMetricsFullHistory], this would equal all past predictions,
     * but in a time-window model [ModelMetricsWithTimeWindow] it reflects only the most recent
     * predictions kept in the buffer.
     */
    val windowTotal: Int
        get() = truePositive400 + falsePositive400 + falseNegative400 + trueNegative400

    /**
     * Estimate the overall accuracy of the model.
     *
     * Accuracy = (TP + TN) / (TP + TN + FP + FN)
     *
     * The proportion of correct predictions
     *
     * See: [Wikipedia: Accuracy and precision](https://en.wikipedia.org/wiki/Accuracy_and_precision)
     */
    fun estimateAccuracy(): Double =
        if (windowTotal > 0) {
            (truePositive400 + trueNegative400).toDouble() / windowTotal
        } else {
            0.0
        }

    /**
     * Estimate the precision of the model when predicting HTTP 400 responses.
     *
     * Precision(400) = TP / (TP + FP)
     *
     * Of all the requests predicted as 400, how many were truly 400.
     *
     * See: [Wikipedia: Precision and recall](https://en.wikipedia.org/wiki/Precision_and_recall)
     */
    fun estimatePrecision400(): Double {
        val denominator = truePositive400 + falsePositive400
        return if (denominator > 0) {
            truePositive400.toDouble() / denominator
        } else {
            0.0
        }
    }

    /**
     * Estimate the Sensitivity (or recall) of the model when predicting HTTP 400 responses.
     *
     * Recall(400) = TP / (TP + FN)
     *
     * Of all the requests that were actually 400,
     * how many the model correctly predicted as 400.
     *
     * See: [Wikipedia: Sensitivity](https://en.wikipedia.org/wiki/Sensitivity_and_specificity)
     */
    fun estimateSensitivity400(): Double {
        val denominator = truePositive400 + falseNegative400
        return if (denominator > 0) {
            truePositive400.toDouble() / denominator
        } else {
            0.0
        }
    }

    /**
     * Estimate the Specificity (True Negative Rate).
     *
     * Specificity = TN / (TN + FP)
     *
     * Of all the requests that were actually not-400, how many the model correctly predicted as not-400.
     * See: [Wikipedia: Specificity](https://en.wikipedia.org/wiki/Sensitivity_and_specificity)
     */
    fun estimateSpecificity(): Double {
        val denominator = trueNegative400 + falsePositive400
        return if (denominator > 0) {
            trueNegative400.toDouble() / denominator
        } else {
            0.0
        }
    }

    /**
     * Estimate the Negative Predictive Value (NPV).
     *
     * NPV = TN / (TN + FN)
     *
     * Of all the requests predicted as not-400, how many were truly not-400.
     * See: [Wikipedia: Negative predictive value](https://en.wikipedia.org/wiki/Positive_and_negative_predictive_values)
     */
    fun estimateNPV(): Double {
        val denominator = trueNegative400 + falseNegative400
        return if (denominator > 0) {
            trueNegative400.toDouble() / denominator
        } else {
            0.0
        }
    }

    /**
     * Estimate the Matthews Correlation Coefficient (MCC) for predicting 400.
     *
     * MCC = (TP * TN - FP * FN) /
     *       sqrt((TP + FP)(TP + FN)(TN + FP)(TN + FN))
     *
     * MCC ranges from -1 to 1:
     * - +1 → perfect prediction
     * -  0 → no better than random
     * - -1 → total disagreement
     *
     * See: [Wikipedia: Matthews correlation coefficient](https://en.wikipedia.org/wiki/Matthews_correlation_coefficient)
     */
    fun estimateMCC400(): Double {
        val tp = truePositive400.toDouble()
        val tn = trueNegative400.toDouble()
        val fp = falsePositive400.toDouble()
        val fn = falseNegative400.toDouble()

        val denominator = sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn))
        return if (denominator > 0) {
            (tp * tn - fp * fn) / denominator
        } else {
            0.0
        }
    }

    /**
     * Return a bundle of all key performance metrics as a [ModelEvaluation].
     *
     * This is the preferred way to query the model’s performance,
     * as it provides a single unified object instead of calling individual metric methods.
     */
    fun estimateMetrics(): ModelEvaluation =
        ModelEvaluation(
            accuracy = estimateAccuracy(),
            precision400 = estimatePrecision400(),
            sensitivity400 = estimateSensitivity400(),
            npv = estimateNPV(),
            specificity = estimateSpecificity(),
            mcc = estimateMCC400()
        )

    /**
     * Update the internal performance counters after a new prediction.
     *
     * @param predictedStatusCode the status code predicted by the model
     * @param actualStatusCode    the true status code obtained after executing the request
     *
     * Notes:
     * - Metrics can only be updated if the actual status code is known.
     * - If a prediction leads to rejecting an input without execution,
     *   no update can be made since the correctness of the prediction is unknown.
     */
    fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int)
}
