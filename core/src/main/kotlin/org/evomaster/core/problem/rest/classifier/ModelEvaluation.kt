package org.evomaster.core.problem.rest.classifier

/**
 * Holds evaluation metrics for a classifier on a given endpoint.
 *
 * - Default values (DEFAULT_NO_DATA) are set to mimic "random guessing":
 *   - accuracy, precision, recall are 0.5
 *   - F1 is computed from precision and recall (defaults to 0.5 as well)
 *   - MCC is 0.0
 *
 * Thus, if an endpoint has unsupported genes, it is treated as no better than flipping a coin.
 * This prevents such endpoints from unfairly reducing the overall performance metrics.
 *
 * For detailed definitions of these metrics, see [ModelMetrics].
 */
data class ModelEvaluation(

    /** Accuracy of the classifier. See [ModelMetrics] for details. */
    val accuracy: Double,

    /** Precision for detecting HTTP 400 responses. See [ModelMetrics]. */
    val precision400: Double,

    /** Recall for detecting HTTP 400 responses. See [ModelMetrics]. */
    val recall400: Double,

    /** Matthews correlation coefficient (MCC) for 400 vs. non-400. See [ModelMetrics]. */
    val mcc: Double
) {
    init {
        require(accuracy in 0.0..1.0) { "Accuracy must be in [0,1], but was $accuracy" }
        require(precision400 in 0.0..1.0) { "Precision must be in [0,1], but was $precision400" }
        require(recall400 in 0.0..1.0) { "Recall must be in [0,1], but was $recall400" }
        require(mcc in -1.0..1.0) { "MCC must be in [-1,1], but was $mcc" }
    }

    /**
     * F1-score, automatically derived from precision and recall.
     *
     * F1(400) = 2 * (Precision(400) * Recall(400)) / (Precision(400) + Recall(400))
     *
     * Harmonic mean of precision and recall,
     * balancing false positives and false negatives.
     *
     * See: [Wikipedia: F-score](https://en.wikipedia.org/wiki/F-score)
     */
    val f1Score400: Double = if (precision400 + recall400 == 0.0) {
        0.0
    } else {
        2 * precision400 * recall400 / (precision400 + recall400)
    }

    companion object {
        /**
         * Immutable default instance to be used when there is no data
         * (e.g., endpoints with unsupported genes).
         */
        val DEFAULT_NO_DATA =
            ModelEvaluation(
                accuracy = 0.5,
                precision400 = 0.5,
                recall400 = 0.5,
                mcc = 0.0
            )
    }
}
