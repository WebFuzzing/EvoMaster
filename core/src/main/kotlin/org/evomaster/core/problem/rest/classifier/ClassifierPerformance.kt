package org.evomaster.core.problem.rest.classifier

/**
 * Classifier state
 *
 * @param correctPrediction the number of correct predictions made by the classifier
 * @param totalSentRequests the total number of requests sent to the classifier
 */
class ClassifierPerformance(
    val correctPrediction: Int,
    val totalSentRequests: Int
) {

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
    fun updatePerformance(predictionIsCorrect: Boolean): ClassifierPerformance {
        val totalCorrectPredictions =
            if (predictionIsCorrect) correctPrediction + 1 else correctPrediction
        val totalSentRequests = totalSentRequests + 1
        return ClassifierPerformance(totalCorrectPredictions, totalSentRequests)
    }

}
