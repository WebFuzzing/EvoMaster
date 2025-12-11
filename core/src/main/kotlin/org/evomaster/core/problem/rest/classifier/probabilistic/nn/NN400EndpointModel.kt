package org.evomaster.core.problem.rest.classifier.probabilistic.nn

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
 * Simple Neural Network model (implemented without external libraries).
 * - One hidden layer with sigmoid activation
 * - Softmax output layer
 * - Online learning (SGD update per sample)
 * Classifies HTTP 400 and not 400 responses.
 */
class NN400EndpointModel(
    endpoint: Endpoint,
    warmup: Int = 10,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    metricType: EMConfig.AIClassificationMetrics = EMConfig.AIClassificationMetrics.TIME_WINDOW,
    private val learningRate: Double = 0.01,
    randomness: Randomness
) : AbstractProbabilistic400EndpointModel(
    endpoint, warmup, dimension, encoderType, metricType, randomness) {

    init {
        // Throw an exception if a non-NORMAL encoder is provided
        if (encoderType != EMConfig.EncoderType.NORMAL) {
            throw IllegalArgumentException(
                "NN400EndpointModel supports only NORMAL encoder, but got: $encoderType"
            )
        }
    }

    // Initialize weights with default values to prevent null
    private val hiddenSize: Int = 16 // size of the hidden layer
    private var weightsInputHidden: Array<DoubleArray> = Array(1) { DoubleArray(hiddenSize) }
    private var weightsHiddenOutput: Array<DoubleArray> = Array(hiddenSize) { DoubleArray(2) }
    private val outputSize = 2 // [class400, classNot400]


    /** Must be called once to initialize the model properties */
    override fun initializeIfNeeded(inputVector: List<Double>) {
        if (!initialized || dimension == null) {
            require(inputVector.isNotEmpty()) { "Input vector cannot be empty" }
            require(warmup > 0) { "Warmup must be positive" }
            dimension = inputVector.size

            // Initialize with proper dimensions
            weightsInputHidden = Array(dimension!!) {
                DoubleArray(hiddenSize) { randomness.nextDouble(-0.1, 0.1) }
            }
            weightsHiddenOutput = Array(hiddenSize) {
                DoubleArray(outputSize) { randomness.nextDouble(-0.1, 0.1) }
            }
            initialized = true
        } else {
            require(inputVector.size == dimension) {
                "Expected input vector of size $dimension but got ${inputVector.size}"
            }
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

        if (encoder.areAllGenesUnSupported()) {
            // skip classification/training if unsupported
            return AIResponseClassification(
                probabilities = mapOf(
                    NOT_400 to 0.5,
                    400 to 0.5)
            )
        }

        initializeIfNeeded(inputVector)

        if (modelMetrics.totalSentRequests < warmup) {
            // Return equal probabilities during warmup
            return AIResponseClassification(
                probabilities = mapOf(
                    NOT_400 to 0.5,
                    400 to 0.5
                )
            )
        }


        val (_, outputProbs) = forward(inputVector.toDoubleArray())

        return AIResponseClassification(
            probabilities = mapOf(
                NOT_400 to outputProbs[1],
                400 to outputProbs[0]
            )
        )
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        verifyEndpoint(input.endpoint)

        // Skip empty action or null response
        if (input.parameters.isEmpty() || output.getStatusCode()==null) {
            return
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        if (encoder.areAllGenesUnSupported() || inputVector.isEmpty()) {
            // Skip training if unsupported or empty
            val predictedStatusCode = if(randomness.nextBoolean()) 400 else NOT_400
            modelMetrics.updatePerformance(predictedStatusCode,output.getStatusCode()?:-1)
            modelMetrics.updatePerformance(predictedStatusCode, output.getStatusCode()?:-1)
            return
        }

        initializeIfNeeded(inputVector)

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        /**
         * Updating classifier metrics such as accuracy and precision based on its prediction
         */
        updateModelMetrics(input, result = output)
        
        val yIndex = if (output.getStatusCode() == 400) 0 else 1
        val target = DoubleArray(outputSize) { if (it == yIndex) 1.0 else 0.0 }

        val (hidden, outputProbs) = forward(inputVector.toDoubleArray())
        backprop(inputVector.toDoubleArray(), hidden, outputProbs, target)
    }

    // Internal helper functions
    private fun forward(x: DoubleArray): Pair<DoubleArray, DoubleArray> {
        if (!initialized) {
            throw IllegalStateException("Model must be initialized before forward pass")
        }

        // Validate input dimensions
        if (x.isEmpty() || x.size != weightsInputHidden.size) {
            throw IllegalArgumentException("Invalid input vector size: ${x.size}, expected: ${weightsInputHidden.size}")
        }

        // Hidden layer (sigmoid activation)
        val hidden = DoubleArray(hiddenSize) { j ->
            sigmoid(x.indices.sumOf { i -> x[i] * weightsInputHidden[i][j] })
        }

        // Output layer (softmax)
        val rawOut = DoubleArray(outputSize) { k ->
            (0 until hiddenSize).sumOf { j -> hidden[j] * weightsHiddenOutput[j][k] }
        }
        
        // Safe softmax implementation
        val maxVal = rawOut.maxOrNull() ?: 0.0
        val expOut = rawOut.map { exp(it - maxVal) }
        val sumExp = expOut.sum().coerceAtLeast(1e-10) // Avoid division by zero
        val probs = expOut.map { it / sumExp }.toDoubleArray()

        return Pair(hidden, probs)
    }

    private fun backprop(x: DoubleArray, hidden: DoubleArray, output: DoubleArray, target: DoubleArray) {
        val wIH = requireNotNull(weightsInputHidden)
        val wHO = requireNotNull(weightsHiddenOutput)

        // Output error
        val outputError = DoubleArray(outputSize) { k -> output[k] - target[k] }

        // Hidden error
        val hiddenError = DoubleArray(hiddenSize) { j ->
            val sum = (0 until outputSize).sumOf { k -> outputError[k] * wHO[j][k] }
            sum * hidden[j] * (1 - hidden[j]) // sigmoid derivative
        }

        // Update weights hidden→output
        for (j in 0 until hiddenSize) {
            for (k in 0 until outputSize) {
                wHO[j][k] -= learningRate * outputError[k] * hidden[j]
            }
        }

        // Update weights input→hidden
        for (i in x.indices) {
            for (j in 0 until hiddenSize) {
                wIH[i][j] -= learningRate * hiddenError[j] * x[i]
            }
        }
    }

}