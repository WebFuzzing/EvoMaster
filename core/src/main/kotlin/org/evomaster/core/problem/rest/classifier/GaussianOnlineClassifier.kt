package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.exp

/**
 * TODO this is work in progress
 */
class GaussianOnlineClassifier(private val dimension: Int=0) : AIModel {

    private val density400 = Density(dimension) // class for 400
    private val density200 = Density(dimension) // class for 200


    // Updating the classifier
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val inputVector = inputEncoder(input)

        // check the compatibility of the input vector and the classifier dimension
        if(inputVector.size != dimension){
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        // Update Gaussian densities
        when (output.getStatusCode()) {
            400 -> density400.update(inputVector)
            200 -> density200.update(inputVector)
            else -> throw IllegalArgumentException("Label must be G_2xx or G_4xx")
        }

    }

    // Gaussian classification
    override fun classify(input: RestCallAction): AIResponseClassification {
        val inputVector = inputEncoder(input)

        // check the compatibility of the input vector and the classifier dimension
        if(inputVector.size != dimension){
            throw IllegalArgumentException("Expected input vector of size $dimension but got ${inputVector.size}")
        }

        val logProbability400 = ln(density400.weight()) + logLikelihood(inputVector, density400)
        val logProbability200 = ln(density200.weight()) + logLikelihood(inputVector, density200)

        val probability400 = exp(logProbability400)
        val probability200 = exp(logProbability200)

        val response = AIResponseClassification(
            probabilities = mapOf(
                400 to probability400,
                200 to probability200
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

    private class Density(dimension: Int) {

        init {
            require(dimension > 0) { "Dimension must be greater than 0 but got $dimension" }
        }

        var n = 0
        val mean = MutableList(dimension) { 0.0 }
        private val M2 = MutableList(dimension) { 0.0 }

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

