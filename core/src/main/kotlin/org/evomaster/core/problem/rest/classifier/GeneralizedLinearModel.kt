package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.exp

/**
 * An online binary classifier for REST API actions using a Generalized Linear Model (logistic regression).
 *
 * This model classifies between HTTP status codes 200 and 400, and updates its weights incrementally.
 * It uses stochastic gradient descent (SGD) to learn the parameters.
 *
 * Assumes binary labels:
 * - Label 1.0 for status code 200
 * - Label 0.0 for status code 400
 *
 * @param dimension the number of features (from input encoding)
 * @param learningRate learning rate for SGD updates
 */
class GLMOnlineClassifier(
    private val dimension: Int = 0,
    private val learningRate: Double = 0.01
) : AIModel {

    private val weights = MutableList(dimension) { 0.0 }
    private var bias = 0.0


    // getter returns the current weights and bias
    fun getModelParams(): List<Double> = weights.toList() + bias

    /** Predicts the probability that the input belongs to class 1 (HTTP 200) */
    override fun classify(input: RestCallAction): AIResponseClassification {
        val x = InputEncoderUtils.encode(input)

        if (x.size != dimension)
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${x.size}")

        val z = x.zip(weights).sumOf { (xi, wi) -> xi * wi } + bias
        val prob200 = sigmoid(z)
        val prob400 = 1.0 - prob200

        return AIResponseClassification(
            probabilities = mapOf(
                200 to prob200,
                400 to prob400
            )
        )
    }

    /** Updates the weights using SGD based on the label from the HTTP response */
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val x = InputEncoderUtils.encode(input)

        if (x.size != dimension)
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${x.size}")

        val y = when (output.getStatusCode()) {
            200 -> 1.0
            400 -> 0.0
            else -> throw IllegalArgumentException("Unsupported label: only 200 and 400 are handled")
        }

        val z = x.zip(weights).sumOf { (xi, wi) -> xi * wi } + bias
        val prediction = sigmoid(z)
        val error = prediction - y

        // SGD update
        for (i in x.indices) {
            weights[i] -= learningRate * error * x[i]
        }
        bias -= learningRate * error
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

}
