package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.exp
import kotlin.random.Random

/**
 * Gaussian classifier for REST API calls.
 *
 * This classifier builds two independent multivariate Gaussian distributions (one for HTTP 200 responses,
 * and one for HTTP 400 responses) based on numeric feature encodings of `RestCallAction` instances.
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
 * - Predicts based on the posterior probabilities (i.e., probabilities sum to 1).
 *
 * ## Assumptions:
 * - The input encoding (`InputEncoderUtils.encode`) produces consistent-length numeric vectors.
 * - The only two supported classes are 200 and 400.
 *
 * @param dimension the fixed dimensionality of the input feature vectors.
 * @param warmup the number of warmup updates to familiarize the classifier with at least a few true observations
 */
class GaussianModel : AbstractAIModel() {

    var density200: Density? = null
    var density400: Density? = null

    /** Must be called once to initialize the model properties */
    fun setup(dimension: Int, warmup: Int) {
        require(dimension > 0 ) { "Dimension must be positive." }
        require(warmup > 0 ) { "Warmup must be positive." }
        this.dimension = dimension
        this.density200 = Density(dimension)
        this.density400 = Density(dimension)
        this.warmup = warmup
    }


    override fun classify(input: RestCallAction): AIResponseClassification {

        if (performance.totalSentRequests < warmup) {
            throw IllegalStateException("Classifier not ready as warmup is not completed.")
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        val logLikelihood200 = ln(this.density200!!.weight()) + logLikelihood(inputVector, this.density200!!)
        val logLikelihood400 = ln(this.density400!!.weight()) + logLikelihood(inputVector, this.density400!!)

        // ensure the outputs as positives
        val likelihood200 = exp(logLikelihood200)
        val likelihood400 = exp(logLikelihood400)

        // Normalize posterior probabilities
        val total = likelihood200 + likelihood400
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
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

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
         * Updating the density functions based on the real observation
         */
        if (trueStatusCode == 400) {
            this.density400!!.update(inputVector)
        } else {
            this.density200!!.update(inputVector)
        }

    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return this.performance.estimateAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        //TODO might need updating
        return this.performance.estimateAccuracy()
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
}
