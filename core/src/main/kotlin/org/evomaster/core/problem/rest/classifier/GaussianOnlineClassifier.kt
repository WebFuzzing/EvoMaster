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
class GaussianOnlineClassifier : AIModel {

    private var dimension: Int? = null
    private var density200: Density? = null
    private var density400: Density? = null

    fun setDimension(d: Int) {
        require(d > 0) { "Dimension must be positive." }
        this.dimension = d
        this.density200 = Density(d)
        this.density400 = Density(d)
    }

    fun getDimension(): Int {
        check(this.dimension != null) { "Classifier not initialized. Call setDimension first." }
        return this.dimension!!
    }

    fun getDensity200(): Density {
        check(this.density200 != null) { "Classifier not initialized. Call setDimension first." }
        return this.density200!!
    }

    fun getDensity400(): Density {
        check(this.density400 != null) { "Classifier not initialized. Call setDimension first." }
        return this.density400!!
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = InputEncoderUtils.encode(input)

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        when (output.getStatusCode()) {
            200 -> this.density200!!.update(inputVector)
            400 -> this.density400!!.update(inputVector)
            else -> throw IllegalArgumentException("Label must be G_2xx or G_4xx")
        }
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val inputVector = InputEncoderUtils.encode(input)

        if (inputVector.size != this.dimension) {
            throw IllegalArgumentException("Expected input vector of size ${this.dimension} but got ${inputVector.size}")
        }

        val logLikelihood200 = ln(this.density200!!.weight()) + logLikelihood(inputVector, this.density200!!)
        val logLikelihood400 = ln(this.density400!!.weight()) + logLikelihood(inputVector, this.density400!!)

        // ensure the outputs as positives
        val likelihood200 = exp(logLikelihood200)
        val likelihood400 = exp(logLikelihood400)

        return AIResponseClassification(
            scores = mapOf(
                200 to likelihood200,
                400 to likelihood400
            )
        )
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
