package org.evomaster.core.problem.rest.classifier

/**
 * Interface for tracking and estimating performance metrics of a model
 * that predicts HTTP response status codes for REST calls.
 *
 * Implementations are expected to maintain internal counters for the confusion matrix:
 * - TP (True Positives): predicted 400 and actual 400
 * - TN (True Negatives): predicted not-400 and actual not-400
 * - FP (False Positives): predicted 400 but actual not-400
 * - FN (False Negatives): predicted not-400 but actual 400
 *
 * From these values, various metrics can be derived, such as:
 * - Accuracy
 * - Precision (for 400s)
 * - Recall (for 400s)
 * - F1 score (for 400s)
 * - MCC (Matthews Correlation Coefficient, for 400 vs. not-400 classification)
 */
interface ModelMetrics {

    /**
     * Estimate the overall accuracy of the model.
     *
     * Accuracy = (TP + TN) / (TP + TN + FP + FN)
     *
     * The proportion of correct predictions
     *
     * See: [Wikipedia: Accuracy and precision](https://en.wikipedia.org/wiki/Accuracy_and_precision)
     */
    fun estimateAccuracy(): Double

    /**
     * Estimate the precision of the model when predicting HTTP 400 responses.
     *
     * Precision(400) = TP / (TP + FP)
     *
     * Of all the requests predicted as 400, how many were truly 400.
     *
     * See: [Wikipedia: Precision and recall](https://en.wikipedia.org/wiki/Precision_and_recall)
     */
    fun estimatePrecision400(): Double

    /**
     * Estimate the recall of the model when predicting HTTP 400 responses.
     *
     * Recall(400) = TP / (TP + FN)
     *
     * Of all the requests that were actually 400,
     * how many the model correctly predicted as 400.
     *
     * See: [Wikipedia: Precision and recall](https://en.wikipedia.org/wiki/Precision_and_recall)
     */
    fun estimateRecall400(): Double

    /**
     * Estimate the Matthews Correlation Coefficient (MCC) for prediction 400.
     *
     * MCC(400) = (TP * TN - FP * FN) / ((TP+FP)(TP+FN)(TN+FP)(TN+FN))^0.5
     *
     * MCC ranges from -1 to 1:
     * - +1 → perfect prediction
     * -  0 → no better than random
     * - -1 → total disagreement
     *
     * Considered one of the best single-value metrics
     * for binary classification, especially with imbalanced data.
     *
     * See: [Wikipedia: Matthews correlation coefficient](https://en.wikipedia.org/wiki/Matthews_correlation_coefficient)
     */
    fun estimateMCC400(): Double

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
            recall400 = estimateRecall400(),
            mcc = estimateMCC400()
        )


    /**
     * Update the internal performance counters after a new prediction.
     *
     * @param predictedStatusCode the status code predicted by the model
     * @param actualStatusCode    the true status code obtained after executing the request
     *
     * Notes:
     * - Accuracy, precision, recall, etc. can only be updated if the actual status code is known.
     * - If a prediction leads to rejecting an input without execution,
     *   no update can be made since the correctness of the prediction is unknown.
     */
    fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int)
}
