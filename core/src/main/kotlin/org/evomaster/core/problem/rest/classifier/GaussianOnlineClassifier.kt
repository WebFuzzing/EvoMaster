package org.evomaster.core.problem.rest.classifier

import com.google.inject.Inject
import com.google.inject.name.Named
import org.evomaster.core.problem.rest.StatusGroup
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import kotlin.math.ln
import kotlin.math.PI

/**
 * TODO this is work in progress
 */
class GaussianOnlineClassifier @Inject constructor(
    @Named("dimension") private val dimension: Int): AIModel {

    // Class invalid stats 400
    private val count0 = Counter(dimension)
    // Class valid stats 200
    private val count1 = Counter(dimension)

    // Extracting numerical features of a RestCallAction as a double vector
    // as the input of a classifier is always a Double vector
    private fun extractFeatures(input: RestCallAction): List<Double> {
        return input.seeTopGenes().mapNotNull { gene ->
            when (gene) {
                is DoubleGene -> gene.value
                is IntegerGene -> gene.value.toDouble()
                else -> null
            }
        }
    }

    // Updating the classifier
    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val values = extractFeatures(input)

        values.size == dimension || error("Invalid input vector due to size mismatch!")

        println("The model is updating with the values: $values")

        when (output.getStatusCode()) {
            400 -> count0.update(values)
            200 -> count1.update(values)
            else -> throw IllegalArgumentException("Label must be G_2xx or G_4xx")
        }

        // Gaussian update logic
    }

    // Gaussian classification
    override fun classify(input: RestCallAction): AIResponseClassification {
        val values = extractFeatures(input)

        //FIXME for inputs, should rather throw IllegalArgumentException
        values.size == dimension || error("Invalid input vector due to size mismatch!")

        // FIXME remove println before requesting PR review
        println("The model is classifying the values: $values")

        // TODO
        return AIResponseClassification()
    }

    // Probability of validity
//    override fun probValidity(input: RestCallAction): Double {
//        val values = extractFeatures(input)
//
//        values.size == dimension || error("Invalid input vector due to size mismatch!")
//
//        return logLikelihood(x=values, stats= count1)
//    }

    fun predict(xD: List<Double>): StatusGroup {
        require(xD.size == dimension) { "Prediction input size must match dimension" }

        val logP0 = ln(count0.weight()) + logLikelihood(xD, count0)
        val logP1 = ln(count1.weight()) + logLikelihood(xD, count1)

        return if (logP1 > logP0) StatusGroup.G_2xx else StatusGroup.G_4xx
    }

    private fun logLikelihood(x: List<Double>, stats: Counter): Double {
        return x.indices.sumOf { i ->
            val mu = stats.mean[i]
            val varI = stats.variance[i].coerceAtLeast(1e-6) // avoid division by zero
            val diff = x[i] - mu
            -0.5 * ln(2 * PI * varI) - (diff * diff) / (2 * varI)
        }
    }

    private class Counter(dimension: Int) {
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

