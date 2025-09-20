package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallResult

interface ModelAccuracy {

    /**
     * Estimate the accuracy of the model
     */
    fun estimateAccuracy() : Double

    /**
     * Estimate the precision of the model
     */
    fun estimatePrecision400(): Double

    /**
     * A new prediction was made.
     * The estimate of the accuracy will be updated based on whether the prediction was correct or not.
     *
     * Note: this can only be computed if, after the prediction, the input is evaluated (i.e., the test case is executed).
     * If a new input is created based on a rejected prediction, when we would not know if that was correct or not.
     */
    fun updatePerformance(predictedStatusCode: Int, actualStatusCode: Int? = null)
}