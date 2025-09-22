package org.evomaster.core.problem.rest.classifier

/**
 * Classifier state
 * @property correctPrediction the number of correct predictions made by the classifier
 * @property totalSentRequests the total number of requests sent to the classifier
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

    /**
     * Calculates the accuracy of the classifier.
     * @return the ratio of correct predictions to total requests.
     */
    override fun estimateAccuracy(): Double {
        return if (totalSentRequests > 0) {
            correctPrediction.toDouble() / totalSentRequests
        } else {
            0.0
        }
    }

    /**
     * Precision for HTTP 400 responses is TP / (TP + FP), where
     * - TP (truePositive400): number of times the classifier correctly predicted a 400 response
     * - FP (falsePositive400): number of times the classifier predicted 400
     * Returns 0.0 if no positive predictions exist.
     */
    override fun estimatePrecision400(): Double {
        return if ((truePositive400 + falsePositive400) > 0) {
            truePositive400.toDouble() / (truePositive400 + falsePositive400)
        } else {
            0.0
        }
    }

    /**
     * Returns a new ClassifierPerformance with updated counts
     * based on whether the latest prediction was correct.
     * @param predictedStatusCode the predicted HTTP response
     * @param result the result of the executed action
     */
    override fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int) {

        val predictionWasCorrect = predictedStatusCode == actualStatusCode
        if (predictionWasCorrect) {
            correctPrediction++
        }
        totalSentRequests++

        /**
         * True positive means predicting a 400 response correctly
         */
        if (actualStatusCode == 400) {
            if (predictionWasCorrect) {
                truePositive400++
            }
        } else {
            if (!predictionWasCorrect) {
                falsePositive400++
            }
        }

    }
}
