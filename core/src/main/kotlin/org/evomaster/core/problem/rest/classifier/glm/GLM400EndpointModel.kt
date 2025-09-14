package org.evomaster.core.problem.rest.classifier.glm

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.ModelAccuracy
import org.evomaster.core.problem.rest.classifier.ModelAccuracyFullHistory
import org.evomaster.core.problem.rest.classifier.ModelAccuracyWithTimeWindow
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
 */
class GLM400EndpointModel (
    val endpoint: Endpoint,
    var warmup: Int = 10,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType= EMConfig.EncoderType.NORMAL,
    val learningRate: Double = 0.01
): AIModel {

    private var initialized = false

    var weights: MutableList<Double>? = null
    var bias: Double = 0.0

    val performance = ModelAccuracyFullHistory()
    val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    /** Must be called once to initialize the model properties */
    private fun initializeIfNeeded(inputVector: List<Double>) {
        if (dimension == null) {
            require(inputVector.isNotEmpty()) { "Input vector cannot be empty" }
            require(warmup > 0) { "Warmup must be positive" }
            dimension = inputVector.size
            // Initialize weights with zeros
            weights = MutableList(inputVector.size) { 0.0 }
        } else {
            require(inputVector.size == dimension) {
                "Expected input vector of size $dimension but got ${inputVector.size}"
            }
            // Initialize weights if they haven't been initialized yet
            if (weights == null) {
                weights = MutableList(dimension!!) { 0.0 }
            }
        }
        initialized = true
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

    override fun classify(input: RestCallAction): AIResponseClassification {
        verifyEndpoint(input.endpoint)

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        initializeIfNeeded(inputVector)

        if (performance.totalSentRequests < warmup) {
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
        val prob200 = sigmoid(z)
        val prob400 = 1.0 - prob200

        return AIResponseClassification(
            probabilities = mapOf(
                200 to prob200,
                400 to prob400
            )
        )

    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        verifyEndpoint(input.endpoint)

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
        if (performance.totalSentRequests < warmup) {
            val guess = Random.nextBoolean()
            performance.updatePerformance(guess)
            modelAccuracy.updatePerformance(guess)
        } else {
            val predicted = classify(input).prediction()
            val predictIsCorrect = (predicted == trueStatusCode)
            performance.updatePerformance(predictIsCorrect)
            modelAccuracy.updatePerformance(predictIsCorrect)
        }

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

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        verifyEndpoint(endpoint)

        return estimateOverallAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {

        if(!initialized){
            //hasn't learned anything yet
            return 0.5
        }

        return modelAccuracy.estimateAccuracy()
    }

    private fun verifyEndpoint(inputEndpoint: Endpoint){
        if(inputEndpoint != endpoint){
            throw IllegalArgumentException("inout endpoint $inputEndpoint is not the same as the model endpoint $endpoint")
        }
    }
}