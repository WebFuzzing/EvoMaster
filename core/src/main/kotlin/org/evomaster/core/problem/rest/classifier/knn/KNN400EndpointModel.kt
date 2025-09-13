package org.evomaster.core.problem.rest.classifier.knn

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
import kotlin.collections.map
import kotlin.math.sqrt
import kotlin.random.Random

class KNN400EndpointModel (
    val endpoint: Endpoint,
    val k: Int = 3,
    var warmup: Int = 10,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType= EMConfig.EncoderType.RAW
): AIModel {

    private var initialized = false

    val samples = mutableListOf<Pair<List<Double>, Int>>()  // (features, statusCode)

    val performance = ModelAccuracyFullHistory()
    val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    // Euclidean distance between two points in the feature space
    private fun distance(a: List<Double>, b: List<Double>): Double {
        return sqrt(a.zip(b).sumOf { (ai, bi) -> (ai - bi) * (ai - bi) })
    }

    /** Must be called once to initialize the model properties */
    private fun initializeIfNeeded(inputVector: List<Double>) {
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
            val classification = classify(input)
            val predicted = classify(input).prediction()
            val predictIsCorrect = (predicted == trueStatusCode)
            performance.updatePerformance(predictIsCorrect)
            modelAccuracy.updatePerformance(predictIsCorrect)
        }

        /**
         * Store only classes of interest (i.e., 200 and 400 groups)
         */
        if (trueStatusCode == 400) {
            samples.add(inputVector to 400)
        } else {
            samples.add(inputVector to 200)
        }

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
