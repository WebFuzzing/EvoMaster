package org.evomaster.core.problem.rest.classifier

interface ModelAccuracy {

    /**
     * Estimate the accuracy of the model
     */
    fun estimateAccuracy() : Double

    /**
     * A new prediction was made.
     * The estimate of the accuracy will be updated based on whether the prediction was correct or not.
     *
     * Note: this can only be computed if, after the prediction, the input is evaluated (ie the test case is executed).
     * If a new input is created based on a rejected prediction, when we would not know if that was correct or not.
     */
    fun updatePerformance(predictionWasCorrect: Boolean)
}