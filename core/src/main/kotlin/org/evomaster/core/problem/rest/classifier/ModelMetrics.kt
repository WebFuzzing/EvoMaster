package org.evomaster.core.problem.rest.classifier

/**
 * Interface for tracking and estimating performance metrics such as accuracy
 * and precision of a model that predicts HTTP response status codes for REST calls.
 * Metrics are updated incrementally as new predictions are made
 * and evaluated against actual execution results.
 */
interface ModelMetrics {

    /**
     * Estimate the overall accuracy of the model.
     * Accuracy is defined as the ratio of correct predictions
     * over the total number of evaluated predictions.
     */
    fun estimateAccuracy() : Double


    /**
     * Estimate the precision of the model when predicting HTTP 400 responses.
     * Precision is defined as the ratio of correctly predicted 400 responses
     * over all predictions where the model predicted 400.
     */
    fun estimatePrecision400(): Double

    /**
     * Update the internal metrics after a new prediction.
     *
     * @param predictedStatusCode the status code predicted by the model
     * @param actualStatusCode the true status code obtained after executing the test case
     *
     * Notes:
     * - Accuracy and precision can only be updated if the actual status code is known
     *   (i.e., after executing the test case).
     * - If a prediction leads to rejecting an input without execution, no update can be made,
     *   since the correctness of the prediction is unknown.
     */
    fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int)
}