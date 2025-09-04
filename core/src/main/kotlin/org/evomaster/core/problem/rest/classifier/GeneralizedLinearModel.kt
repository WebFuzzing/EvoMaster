package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.exp
import kotlin.random.Random

/**
 * An online binary classifier for REST API actions using a Generalized Linear Model (logistic regression).
 *
 * This model classifies between HTTP status codes 200 and 400, and updates its weight incrementally.
 * It uses stochastic gradient descent (SGD) to learn the parameters.
 *
 * Assumes binary labels:
 * - Label 1.0 for status code 200
 * - Label 0.0 for status code 400
 *
 * @param dimension the number of features (from input encoding)
 * @param learningRate learning rate for SGD updates
 * @param warmup the number of warmup updates to familiarize the classifier with at least a few true observations
 */
class GLMOnlineClassifier(
    private val learningRate: Double = 0.01
) : AIModel {

    var warmup: Int = 10
    var dimension: Int? = null
    var weights: MutableList<Double>? = null
    var bias: Double = 0.0
    var performance: ModelAccuracyFullHistory = ModelAccuracyFullHistory()

    /** Must be called once to initialize the model properties */
    fun setup(dimension: Int, warmup: Int) {
        require(dimension > 0) { "Dimension must be positive." }
        this.dimension = dimension
        this.weights = MutableList(dimension) { 0.0 }
        require(warmup > 0 ) { "Warmup must be positive." }
        this.warmup = warmup
    }

    fun getModelParams(): List<Double> {
        check(weights != null) { "Classifier not initialized. Call setDimension first." }
        return weights!!.toList() + bias
    }

    override fun classify(input: RestCallAction): AIResponseClassification {

        if (performance.totalSentRequests< warmup) {
            throw IllegalStateException("Classifier not ready as warmup is not completed.")
        }

        val inputVector = InputEncoderUtils.encode(input).normalizedEncodedFeatures

        val dim = dimension ?: throw IllegalStateException("Dimension not set. Call setDimension() first.")
        if (inputVector.size != dim) throw IllegalArgumentException("Expected input vector of size $dim but got ${inputVector.size}")

        val z = inputVector.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prob200 = sigmoid(z)
        val prob400 = 1.0 - prob200

        return AIResponseClassification(
            probabilities = mapOf(
                200 to prob200,
                400 to prob400
            )
        )
    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return this.performance.estimateAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        //TODO might need updating
        return this.performance.estimateAccuracy()
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = InputEncoderUtils.encode(input).normalizedEncodedFeatures

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        /**
         * Updating classifier performance based on its prediction
         * Before the warmup is completed, the update is based on a crude guess (like a coin flip).
         */
        val trueStatusCode = output.getStatusCode()
        if (performance.totalSentRequests < warmup) {
            performance.updatePerformance(Random.nextBoolean())
        } else {
            val predicted = classify(input).prediction()
            performance.updatePerformance(predicted == trueStatusCode)
        }

        /**
         * Updating model parameters
         */
        val y = when (trueStatusCode) {
            in 200..299 -> 1.0
            in 400..499 -> 0.0
            else -> throw IllegalArgumentException("Unsupported label: only 200 and 400 are handled")
        }
        val z = inputVector.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prediction = sigmoid(z)
        val error = prediction - y

        for (i in inputVector.indices) {
            weights!![i] -= learningRate * error * inputVector[i]
        }
        bias -= learningRate * error
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))
}
