package org.evomaster.core.problem.rest.classifier.quantifier

/**
 * Holds evaluation metrics for a classifier on a given endpoint.
 *
 * - Default values (DEFAULT_NO_DATA) are set to represent "no useful signal":
 *   - accuracy, precision, recall, NPV, specificity are 0.0
 *   - F1 is derived from precision and recall
 *   - MCC is 0.0
 *
 * Thus, if an endpoint has unsupported genes, it is treated as providing
 * no actionable information and will not influence downstream decisions.
 *
 * For detailed definitions of these metrics, see [ModelMetrics].
 */
data class ModelEvaluation(

    /** Accuracy of the classifier. */
    val accuracy: Double,

    /** Precision for detecting HTTP 400 responses (TP / (TP + FP)). */
    val precision400: Double,

    /** Sensitivity for detecting HTTP 400 responses (TP / (TP + FN)). */
    val sensitivity400: Double,

    /**
     * Specificity (True Negative Rate) for non-400 responses (TN / (TN + FP)).
     * Indicates how well the classifier avoids falsely flagging valid requests.
     */
    val specificity: Double,

    /**
     * Negative Predictive Value (NPV) for non-400 responses (TN / (TN + FN)).
     * Indicates how reliable a negative (non-400) prediction is.
     */
    val npv: Double,

    /** Matthews correlation coefficient (MCC) for 400 vs. non-400. */
    val mcc: Double
) {

    init {
        require(accuracy in 0.0..1.0) {
            "Accuracy must be in [0,1], but was $accuracy"
        }
        require(precision400 in 0.0..1.0) {
            "Precision must be in [0,1], but was $precision400"
        }
        require(sensitivity400 in 0.0..1.0) {
            "Recall must be in [0,1], but was $sensitivity400"
        }
        require(specificity in 0.0..1.0) {
            "Specificity must be in [0,1], but was $specificity"
        }
        require(npv in 0.0..1.0) {
            "NPV must be in [0,1], but was $npv"
        }
        require(mcc in -1.0..1.0) {
            "MCC must be in [-1,1], but was $mcc"
        }
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
     * Harmonic mean of precision and recall, emphasizing correctness
     * of positive (400) predictions.
     */
    val f1Score400: Double = if (precision400 + sensitivity400 == 0.0) {
        0.0
    } else {
        2 * precision400 * sensitivity400 / (precision400 + sensitivity400)
    }

    companion object {

        /**
         * Immutable default instance used when no evaluation data is available
         * (e.g., endpoints with unsupported genes).
         */
        val DEFAULT_NO_DATA =
            ModelEvaluation(
                accuracy = 0.0,
                precision400 = 0.0,
                sensitivity400 = 0.0,
                specificity = 0.0,
                npv = 0.0,
                mcc = 0.0
            )
    }
}
