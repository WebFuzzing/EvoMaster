package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.ModelAccuracy
import org.evomaster.core.problem.rest.classifier.ModelAccuracyFullHistory
import org.evomaster.core.problem.rest.classifier.ModelAccuracyWithTimeWindow
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.search.service.Randomness

/**
 * Base class for all probabilistic classifiers working at an endpoint (400 vs. not 400).
 *
 * Provides:
 * - Common properties (endpoint, warmup, encoderType, randomness, dimension, initialized flag)
 * - Shared methods for initialization checks and accuracy estimation
 */
abstract class AbstractProbabilistic400EndpointModel(
    val endpoint: Endpoint,
    var warmup: Int = 10,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    val randomness: Randomness
) : AIModel {

    protected var initialized: Boolean = false

    val modelAccuracyFullHistory: ModelAccuracyFullHistory = ModelAccuracyFullHistory()
    val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    /** Ensure endpoint matches this model */
    protected fun verifyEndpoint(inputEndpoint: Endpoint) {
        if (inputEndpoint != endpoint) {
            throw IllegalArgumentException("Input endpoint $inputEndpoint does not match model endpoint $endpoint")
        }
    }

    /** Initialize dimension once, validate consistency */
    open fun initializeIfNeeded(inputVector: List<Double>) {
        if (dimension == null) {
            require(inputVector.isNotEmpty()) { "Input vector cannot be empty" }
            require(warmup > 0) { "Warmup must be positive" }
            dimension = inputVector.size
        } else {
            require(inputVector.size == dimension) {
                "Expected input vector of size $dimension but got ${inputVector.size}"
            }
        }
        initialized = true
    }

    /**
     * Updating classifier performance based on its prediction
     * Before the warmup is completed, the update is based on a crude guess (like a coin flip).
     */
    protected fun updatePerformance(input: RestCallAction, outputStatusCode: Int?) {

        if (modelAccuracyFullHistory.totalSentRequests < warmup || input.parameters.isEmpty()) {
            val guess = randomness.nextBoolean()
            modelAccuracyFullHistory.updatePerformance(guess)
            modelAccuracy.updatePerformance(guess)
        } else {
            val predicted = classify(input).prediction()
            val predictIsCorrect = (predicted == outputStatusCode)
            modelAccuracyFullHistory.updatePerformance(predictIsCorrect)
            modelAccuracy.updatePerformance(predictIsCorrect)
        }

    }


    /** Default accuracy estimates */
    override fun estimateAccuracy(endpoint: Endpoint): Double {
        verifyEndpoint(endpoint)
        return estimateOverallAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        if (!initialized) {
            // hasn’t learned anything yet
            return 0.5
        }
        return modelAccuracy.estimateAccuracy()
    }
}
