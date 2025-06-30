package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.exp

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
 * - Maintains two `Density` instances: one for class 200 (successful responses) and one for class 400 (error responses).
 * - Computes log-likelihood under each distribution and classifies based on maximum likelihood with class priors.
 *
 * ## Assumptions:
 * - The input encoding (`InputEncoderUtils.encode`) produces consistent-length numeric vectors.
 * - The only two supported classes are 200 and 400.
 *
 * @param dimension the fixed dimensionality of the input feature vectors.
 */
class GaussianOnlineClassifier(private val dimension: Int=0) : AIModel {

    private val density200 = Density(dimension) // class for 200
    private val density400 = Density(dimension) // class for 400

    // getter
    fun getDensity200(): Density = density200
    fun getDensity400(): Density = density400

    /** Updates the classifier using a request of type `RestCallAction` and its result of type `RestCallResult` */
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = InputEncoderUtils.encode(input)

        // check the compatibility of the input vector and the classifier dimension
        if(inputVector.size != dimension){
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        // Update Gaussian densities
        when (output.getStatusCode()) {
            200 -> density200.update(inputVector)
            400 -> density400.update(inputVector)
            else -> throw IllegalArgumentException("Label must be G_2xx or G_4xx")
        }

    }

    /** Classifies a request of type `RestCallAction` using Gaussian classification */
    override fun classify(input: RestCallAction): AIResponseClassification {
        val inputVector = InputEncoderUtils.encode(input)

        // check the compatibility of the input vector and the classifier dimension
        if(inputVector.size != dimension){
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        val logProbability200 = ln(density200.weight()) + logLikelihood(inputVector, density200)
        val logProbability400 = ln(density400.weight()) + logLikelihood(inputVector, density400)

        val probability200 = exp(logProbability200)
        val probability400 = exp(logProbability400)

        val response = AIResponseClassification(
            probabilities = mapOf(
                200 to probability200,
                400 to probability400
            )
        )

        return response
    }

    private fun logLikelihood(x: List<Double>, stats: Density): Double {
        return x.indices.sumOf { i ->
            val mu = stats.mean[i]
            val varI = stats.variance[i].coerceAtLeast(1e-6) // avoid division by zero
            val diff = x[i] - mu
            -0.5 * ln(2 * PI * varI) - (diff * diff) / (2 * varI)
        }
    }

    class Density(dimension: Int) {

        var n = 0
        val mean = MutableList(dimension) { 0.0 }
        val M2 = MutableList(dimension) { 0.0 }

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

