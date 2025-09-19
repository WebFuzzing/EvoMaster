package org.evomaster.core.problem.rest.classifier.probabilistic.glm

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import kotlin.math.exp


/**
 * An online binary classifier for REST API actions using a Generalized Linear Model (logistic regression).
 *
 * This model classifies between HTTP status codes 400 and not 400, and updates its weight incrementally.
 * It uses stochastic gradient descent (SGD) to learn the parameters.
 */
class GLM400EndpointModel(
    endpoint: Endpoint,
    warmup: Int = 10,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    private val learningRate: Double = 0.01,
    randomness: Randomness
) : AbstractProbabilistic400EndpointModel(endpoint, warmup, dimension, encoderType, randomness) {

    private var weights: MutableList<Double>? = null
    private var bias: Double = 0.0

    /** Initialize dimension and weights if needed */
    override fun initializeIfNeeded(inputVector: List<Double>) {
        super.initializeIfNeeded(inputVector)
        if (weights == null) {
            weights = MutableList(dimension!!) { 0.0 }
        }
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

    override fun classify(input: RestCallAction): AIResponseClassification {
        verifyEndpoint(input.endpoint)

        // treat empty action as "unknown", avoid touching the model
        if (input.parameters.isEmpty()) {
            return AIResponseClassification()
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        initializeIfNeeded(inputVector)

        if (modelAccuracyFullHistory.totalSentRequests < warmup) {
            // Return equal probabilities during warmup
            return AIResponseClassification(
                probabilities = mapOf(
                    200 to 0.5,
                    400 to 0.5
                )
            )
        }

        if (inputVector.size != dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        val z = inputVector.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prob400 = 1 - sigmoid(z)
        val probNot400 = 1.0 - prob400

        return AIResponseClassification(
            probabilities = mapOf(
                200 to probNot400,
                400 to prob400
            )
        )

    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        verifyEndpoint(input.endpoint)

        // Ignore empty action
        if (input.parameters.isEmpty()) {
            return
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        initializeIfNeeded(inputVector)

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        /**
         * Updating classifier performance based on its prediction
         * Before the warmup is completed, the update is based on a crude guess (like a coin flip).
         */
        val trueStatusCode = output.getStatusCode()
        updatePerformance(input, trueStatusCode)

        /**
         * Updating model parameters
         */
        val y = if (trueStatusCode == 400) 0.0 else 1.0

        val z = inputVector.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prediction = sigmoid(z)
        val error = prediction - y

        for (i in inputVector.indices) {
            weights!![i] -= learningRate * error * inputVector[i]
        }
        bias -= learningRate * error

    }

    fun getModelParams(): List<Double> {
        check(weights != null) { "Classifier not initialized. Call setDimension first." }
        return weights!!.toList() + bias
    }

}