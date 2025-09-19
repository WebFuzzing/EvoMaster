package org.evomaster.core.problem.rest.classifier.probabilistic.gaussian

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln

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
 * - Maintains two `Density` instances: one for class 400 and one for class not-400.
 * - Computes the log-likelihood of the input under each class-specific Gaussian.
 * - Combines likelihoods with class priors using Bayes' rule to calculate normalized posterior probabilities.
 * - Predicts based on the posterior probabilities.
 *
 * ## Assumptions:
 * - The input encoding (`InputEncoderUtils.encode`) produces consistent-length numeric vectors.
  */
class Gaussian400EndpointModel (
    endpoint: Endpoint,
    warmup: Int = 10,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType= EMConfig.EncoderType.NORMAL,
    randomness: Randomness
): AbstractProbabilistic400EndpointModel(endpoint, warmup, dimension, encoderType, randomness) {

    var density400: Density? = null
    private set

    var densityNot400: Density? = null
        private set


    /** Must be called once to initialize the model properties
     * Initialize dimension and weights if needed
     */
    override fun initializeIfNeeded(inputVector: List<Double>) {
        super.initializeIfNeeded(inputVector)
        if(density400 == null) {
            density400 = Density(dimension!!)
        }
        if(densityNot400 == null) {
            densityNot400 = Density(dimension!!)
        }
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

        val logLikelihood400 = ln(density400!!.weight()) + logLikelihood(inputVector, density400!!)
        val logLikelihoodNot400 = ln(densityNot400!!.weight()) + logLikelihood(inputVector, densityNot400!!)

        // ensure the outputs as positives
        val likelihood400 = exp(logLikelihood400)
        val likelihoodNot400 = exp(logLikelihoodNot400)

        // Normalize posterior probabilities
        val total = likelihoodNot400 + likelihood400

        // Handle the case when both likelihoods are zero
        if (total == 0.0 || total.isNaN() || likelihood400.isNaN() || likelihoodNot400.isNaN()) {
            return AIResponseClassification(
                probabilities = mapOf(
                    200 to 0.5,
                    400 to 0.5
                )
            )
        }

        val posterior400 = likelihood400 / total
        val posteriorNot400 = likelihoodNot400 / total

        return AIResponseClassification(
            probabilities = mapOf(
                200 to posteriorNot400,
                400 to posterior400
            )
        )
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        verifyEndpoint(input.endpoint)

        // Ignore empty action
        if (input.parameters.isEmpty()) {
            return
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()

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
         * Updating the density functions based on the real observation
         */
        if (trueStatusCode == 400) {
            density400!!.update(inputVector)
        } else {
            densityNot400!!.update(inputVector)
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

}
