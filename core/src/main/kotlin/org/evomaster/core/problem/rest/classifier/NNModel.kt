package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.exp
import kotlin.random.Random

/**
 * Simple Neural Network model (implemented without external libraries).
 * - One hidden layer with sigmoid activation
 * - Softmax output layer
 * - Online learning (SGD update per sample)
 *
 * Classifies HTTP 400 response.
 */
class NNModel(
    private val learningRate: Double = 0.01,
    private val hiddenSize: Int = 16
) : AbstractAIModel() {

    private var weightsInputHidden: Array<DoubleArray>? = null
    private var weightsHiddenOutput: Array<DoubleArray>? = null
    private val outputSize = 2 // [class400, class200]

    /** Initialize weights */
    fun setup(dimension: Int, warmup: Int) {
        require(dimension > 0) { "Dimension must be positive." }
        require(warmup > 0) { "Warmup must be positive." }
        this.dimension = dimension
        this.warmup = warmup

        // Xavier initialization
        weightsInputHidden = Array(dimension) { DoubleArray(hiddenSize) { Random.nextDouble(-0.1, 0.1) } }
        weightsHiddenOutput = Array(hiddenSize) { DoubleArray(outputSize) { Random.nextDouble(-0.1, 0.1) } }
    }

    private fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val d = requireNotNull(dimension) { "Model not initialized. Call setup(d, warmup)." }
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode().toDoubleArray()
        require(inputVector.size == d) { "Encoded input size ${inputVector.size} != expected dimension $d" }

        val trueStatusCode = output.getStatusCode()
        val yIndex = if (trueStatusCode == 200) 1 else 0
        val target = DoubleArray(outputSize) { if (it == yIndex) 1.0 else 0.0 }

        // Forward pass
        val (hidden, outputProbs) = forward(inputVector)

        // Backpropagation
        backprop(inputVector, hidden, outputProbs, target)

        // Update performance stats
        if (performance.totalSentRequests < warmup) {
            performance.updatePerformance(Random.nextBoolean())
        } else {
            val predicted = if (outputProbs[1] > outputProbs[0]) 200 else 400
            performance.updatePerformance(predicted == trueStatusCode)
        }
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val d = requireNotNull(dimension) { "Model not initialized. Call setup(d, warmup)." }
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode().toDoubleArray()
        require(inputVector.size == d) { "Encoded input size ${inputVector.size} != expected dimension $d" }

        val (_, outputProbs) = forward(inputVector)

        return AIResponseClassification(
            probabilities = mapOf(
                200 to outputProbs[1],
                400 to outputProbs[0]
            )
        )
    }

    // Internal helper functions
    private fun forward(x: DoubleArray): Pair<DoubleArray, DoubleArray> {
        val wIH = requireNotNull(weightsInputHidden)
        val wHO = requireNotNull(weightsHiddenOutput)

        // Hidden layer (sigmoid activation)
        val hidden = DoubleArray(hiddenSize)
        for (j in 0 until hiddenSize) {
            hidden[j] = sigmoid(x.indices.sumOf { i -> x[i] * wIH[i][j] })
        }

        // Output layer (softmax)
        val rawOut = DoubleArray(outputSize)
        for (k in 0 until outputSize) {
            rawOut[k] = hidden.indices.sumOf { j -> hidden[j] * wHO[j][k] }
        }
        val expOut = rawOut.map { exp(it) }
        val sumExp = expOut.sum()
        val probs = expOut.map { it / sumExp }.toDoubleArray()

        return hidden to probs
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
