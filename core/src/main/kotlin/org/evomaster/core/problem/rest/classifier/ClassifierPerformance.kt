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
}
