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
    private val learningRate: Double = 0.01
) : AIModel {

    private var dimension: Int? = null
    private var weights: MutableList<Double>? = null
    private var bias: Double = 0.0

    /** Must be called once to initialize model weights based on dimension */
    fun setDimension(d: Int) {
        require(d > 0) { "Dimension must be positive." }
        dimension = d
        weights = MutableList(d) { 0.0 }
        bias = 0.0
    }

    fun getModelParams(): List<Double> {
        check(weights != null) { "Classifier not initialized. Call setDimension first." }
        return weights!!.toList() + bias
    }

    fun getDimension(): Int {
        check(this.dimension != null) { "Classifier not initialized. Call setDimension first." }
        return this.dimension!!
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val x = InputEncoderUtils.encode(input)

        val dim = dimension ?: throw IllegalStateException("Dimension not set. Call setDimension() first.")
        if (x.size != dim) throw IllegalArgumentException("Expected input vector of size $dim but got ${x.size}")

        val z = x.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prob200 = sigmoid(z)
        val prob400 = 1.0 - prob200

        return AIResponseClassification(
            scores = mapOf(
                200 to prob200,
                400 to prob400
            )
        )
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val x = InputEncoderUtils.encode(input)

        val dim = dimension ?: throw IllegalStateException("Dimension not set. Call setDimension() first.")
        if (x.size != dim) throw IllegalArgumentException("Expected input vector of size $dim but got ${x.size}")

        val y = when (output.getStatusCode()) {
            200 -> 1.0
            400 -> 0.0
            else -> throw IllegalArgumentException("Unsupported label: only 200 and 400 are handled")
        }

        val z = x.zip(weights!!) { xi, wi -> xi * wi }.sum() + bias
        val prediction = sigmoid(z)
        val error = prediction - y

        for (i in x.indices) {
            weights!![i] -= learningRate * error * x[i]
        }
        bias -= learningRate * error
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))
}
