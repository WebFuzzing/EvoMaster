package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * K-Nearest Neighbors (KNN) classifier for REST API calls.
 *
 * Keeps past observations (feature vector + label).
 * Classifies a new input based on the majority class of the k nearest stored samples.
 */
class KNNModel(private val k: Int = 3) : AbstractAIModel() {

    private val samples = mutableListOf<Pair<List<Double>, Int>>()  // (features, statusCode)

    fun setup(dimension: Int, warmup: Int) {
        require(dimension > 0) { "Dimension must be positive." }
        require(warmup > 0) { "Warmup must be positive." }
        this.dimension = dimension
        this.warmup = warmup
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        if (performance.totalSentRequests < warmup) {
            throw IllegalStateException("Classifier not ready as warmup is not completed.")
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        if (samples.isEmpty()) {
            throw IllegalStateException("No samples available for classification")
        }

        // Find k nearest neighbors
        val neighbors = samples
            .map { (x, label) -> distance(inputVector, x) to label }
            .sortedBy { it.first }
            .take(k)

        // Majority vote
        val votes = neighbors.groupingBy { it.second }.eachCount()
        val predictedClass = votes.maxByOrNull { it.value }!!.key

        // Convert to pseudo probabilities (votes / k)
        val probabilities = votes.mapValues { it.value.toDouble() / k }

        return AIResponseClassification(probabilities = probabilities)
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        val trueStatusCode: Int = output.getStatusCode()
            ?: throw IllegalArgumentException("Status code cannot be null")

        // Update accuracy performance
        if (performance.totalSentRequests < warmup) {
            performance.updatePerformance(Random.nextBoolean())
        } else {
            val predicted = classify(input).prediction()
            performance.updatePerformance(predicted == trueStatusCode)
        }

        // Store only classes of interest (i.e., 200 and 400 groups)
        if (trueStatusCode == 400) {
            samples.add(inputVector to 400)
        } else {
            samples.add(inputVector to 200)
        }

    }


    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return performance.estimateAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        return performance.estimateAccuracy()
    }

    private fun distance(a: List<Double>, b: List<Double>): Double {
        return sqrt(a.zip(b).sumOf { (ai, bi) -> (ai - bi) * (ai - bi) })
    }
}
