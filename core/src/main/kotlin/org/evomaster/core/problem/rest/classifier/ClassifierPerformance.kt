package org.evomaster.core.problem.rest.classifier

/**
 * Classifier state
 * @property correctPrediction the number of correct predictions made by the classifier
 * @property totalSentRequests the total number of requests sent to the classifier
 */
class ClassifierPerformance {

    var correctPrediction: Int = 0
        private set
    var totalSentRequests: Int = 1
        private set

    /**
     * Calculates the accuracy of the classifier.
     * @return the ratio of correct predictions to total requests.
     */
    fun accuracy(): Double {
        return if (totalSentRequests > 0) {
            correctPrediction.toDouble() / totalSentRequests
        } else {
            0.0
        }
    }

    /**
     * Returns a new ClassifierPerformance with updated counts
     * based on whether the latest prediction was correct.
     */
    fun updatePerformance(predictionIsCorrect: Boolean) {
        if (predictionIsCorrect) {
            correctPrediction++
        }
        totalSentRequests++
    }
}
