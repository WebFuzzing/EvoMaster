package org.evomaster.core.problem.rest.classifier

/**
 * Tracks the full history of classifier performance metrics,
 * without any time window (cumulative statistics).
 *
 * Maintains confusion-matrix style counters for binary classification:
 * - Positive class = HTTP 400 responses
 * - Negative class = all other responses
 */
class ModelMetricsFullHistory : ModelMetrics {

    /** Lifetime counter of total evaluated requests. Important for warm-up logic. */
    override var totalSentRequests: Int = 0

    override var truePositive400: Int = 0
    override var falsePositive400: Int = 0
    override var falseNegative400: Int = 0
    override var trueNegative400: Int = 0

    /**
     * Update the performance counters after a new prediction.
     *
     * @param predictedStatusCode the predicted HTTP response
     * @param actualStatusCode    the actual HTTP response
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {
        when {
            actualStatusCode == 400 && predictedStatusCode == 400 -> truePositive400++
            actualStatusCode == 400 && predictedStatusCode != 400 -> falseNegative400++
            actualStatusCode != 400 && predictedStatusCode == 400 -> falsePositive400++
            actualStatusCode != 400 && predictedStatusCode != 400 -> trueNegative400++
        }

        totalSentRequests++

    }
}
