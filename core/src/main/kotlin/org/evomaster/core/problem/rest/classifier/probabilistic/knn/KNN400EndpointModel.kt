package org.evomaster.core.problem.rest.classifier.probabilistic.knn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import kotlin.collections.map
import kotlin.math.sqrt

/**
 * K-Nearest Neighbors (KNN) classifier for REST API calls.
 *
 * Keeps past observations (feature vector + label).
 * Classifies a new input based on the majority class of the k nearest stored samples.
 */
class KNN400EndpointModel (
    endpoint: Endpoint,
    warmup: Int = 10,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType= EMConfig.EncoderType.RAW,
    private val k: Int = 3,
    randomness: Randomness
): AbstractProbabilistic400EndpointModel(endpoint, warmup, dimension, encoderType, randomness) {

    companion object {
        private const val NOT_400 = 200
    }

    /**
     * Stores the training samples for this endpoint model.
     * Each element is a pair of:
     *  - List<Double>: the encoded feature vector
     *  - Int         : the corresponding status code (i.e., HTTP response)
     */
    val samples = mutableListOf<Pair<List<Double>, Int>>()

    // Euclidean distance between two points in the feature space
    private fun distance(a: List<Double>, b: List<Double>): Double {
        return sqrt(a.zip(b).sumOf { (ai, bi) -> (ai - bi) * (ai - bi) })
    }

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

        // Find k nearest neighbors
        val neighbors = samples
            .map { (x, label) -> distance(inputVector, x) to label }
            .sortedBy { it.first }
            .take(k)

        // Majority vote
        val votes = neighbors.groupingBy { it.second }.eachCount()

        // Convert to pseudo probabilities (votes / k)
        val probabilities = votes.mapValues { it.value.toDouble() / k }

        return AIResponseClassification(probabilities = probabilities)
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        verifyEndpoint(input.endpoint)

        // Ignore empty action
        if (input.parameters.isEmpty()) {
            return
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        var inputVector = encoder.encode()

        initializeIfNeeded(inputVector)

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        /**
         * Updating classifier performance based on its prediction
         */
        val trueStatusCode = output.getStatusCode()
        updatePerformance(input, trueStatusCode)

        /**
         * Store only classes of interest (i.e., 400 and not 400 groups)
         */
        if (trueStatusCode == 400) {
            samples.add(inputVector to 400)
        } else {
            samples.add(inputVector to NOT_400)
        }

    }

}
