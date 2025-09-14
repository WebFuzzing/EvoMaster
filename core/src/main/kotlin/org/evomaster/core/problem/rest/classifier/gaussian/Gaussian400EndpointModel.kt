package org.evomaster.core.problem.rest.classifier.gaussian

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
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random


/**
 * Gaussian classifier for REST API calls.
 *
 * This classifier builds two independent multivariate Gaussian distributions (for 400 responses or not 400)
 * based on numeric feature encodings of `RestCallAction` instances.
 *
 * ## Features:
 * - Learns in an *online* fashion: it updates its parameters incrementally as new samples arrive.
 * - Uses the encoded input vectors from REST actions.
 * - Assumes *diagonal covariance*, i.e., each feature dimension is treated independently.
 *
 * ## Internals:
 * - Maintains two `Density` instances: one for class 200 and one for class 400.
 * - Computes the log-likelihood of the input under each class-specific Gaussian.
 * - Combines likelihoods with class priors using Bayes' rule to calculate normalized posterior probabilities.
 * - Predicts based on the posterior probabilities.
 *
 * ## Assumptions:
 * - The input encoding (`InputEncoderUtils.encode`) produces consistent-length numeric vectors.
  */
class Gaussian400EndpointModel (
    val endpoint: Endpoint,
    var warmup: Int = 10,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType= EMConfig.EncoderType.NORMAL
): AIModel {

    private var initialized = false

    val performance = ModelAccuracyFullHistory()
    val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    var density200: Density? = null
    var density400: Density? = null

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
        if(density200 == null) {
            density200 = Density(dimension!!)
        }
        if(density400 == null) {
            density400 = Density(dimension!!)
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

        val logLikelihood200 = ln(density200!!.weight()) + logLikelihood(inputVector, density200!!)
        val logLikelihood400 = ln(density400!!.weight()) + logLikelihood(inputVector, density400!!)

        // ensure the outputs as positives
        val likelihood200 = exp(logLikelihood200)
        val likelihood400 = exp(logLikelihood400)

        // Normalize posterior probabilities
        val total = likelihood200 + likelihood400

        // Handle the case when both likelihoods are zero
        if (total == 0.0) {
            return AIResponseClassification(
                probabilities = mapOf(
                    200 to 0.5,
                    400 to 0.5
                )
            )
        }

        val posterior200 = likelihood200 / total
        val posterior400 = likelihood400 / total

        return AIResponseClassification(
            probabilities = mapOf(
                200 to posterior200,
                400 to posterior400
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
         * Updating the density functions based on the real observation
         */
        if (trueStatusCode == 400) {
            density400!!.update(inputVector)
        } else {
            density200!!.update(inputVector)
        }

    }

    private fun logLikelihood(x: List<Double>, stats: Density): Double {
        return x.indices.sumOf { i ->
            val mu = stats.mean[i]
            val varI = stats.variance[i].coerceAtLeast(1e-6)
            val diff = x[i] - mu
            -0.5 * ln(2 * PI * varI) - (diff * diff) / (2 * varI)
        }
    }

    class Density(dimension: Int) {
        var n = 0
        val mean = MutableList(dimension) { 0.0 }
        val M2 = MutableList(dimension) { 1.0 }

        fun update(x: List<Double>) {
            n++
            for (i in x.indices) {
                val xi = x[i]
                val delta = xi - mean[i]
                mean[i] += delta / n
                M2[i] += delta * (xi - mean[i])
            }
        }

        val variance: List<Double>
            get() = M2.mapIndexed { _, m2i -> if (n > 1) m2i / (n - 1) else 1.0 }

        fun weight() = n.toDouble()
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
