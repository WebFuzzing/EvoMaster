package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class NaiveGaussian1DClassifier: AIModel {

    private var n = 1
    private var mu:Double = 0.0 //initial mean
    private var M2 = 10.0 //initial variance
    private val rng = Random.Default

    private val minValue: Double = -5_000.0
    private val maxValue: Double = 5_000.0

    // Rejection tracking
    private val recentRejects = mutableListOf<Double>()
    private val maxRejectsStored = 20
    private val rejectionWeight = 0.1  // how strongly to move away

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        val result= output.getStatusCode()
        val features = extractFeatures(input)
        if (result == 200) {
            updateAccepted(features.first())
        }
        else if (result == 400) {
            updateRejected(features.first())
        }
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val features = extractFeatures(input)
        val x = features.firstOrNull() ?: return AIResponseClassification(mapOf(400 to 1.0))

        if (n < 2) return AIResponseClassification(mapOf(400 to 1.0)) // not enough data

        val variance = M2 / (n - 1)
        if (variance == 0.0) return AIResponseClassification(mapOf(400 to 1.0))

        val stdDev = sqrt(variance)
        val p200 = gaussianPdf(x, mu, stdDev)

        // Normalize p200 to get probabilities for 200 and 400
        val epsilon = 1e-10
        val p400 = epsilon
        val sum = p200 + p400

        return AIResponseClassification(
            mapOf(
                200 to p200 / sum,
                400 to p400 / sum
            )
        )
    }

    private fun gaussianPdf(x: Double, mean: Double, stdDev: Double): Double {
        val exponent = -((x - mean).pow(2)) / (2 * stdDev * stdDev)
        return (1.0 / (stdDev * sqrt(2 * Math.PI))) * exp(exponent)
    }


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

    private fun updateAccepted(x: Double) {

        n += 1
        val delta = x - mu
        mu += delta / n
        M2 += delta * (x - mu)

        mu = floor(mu).coerceAtMost(maxValue - 1.0).coerceAtLeast(minValue + 1.0)
    }

    private fun updateRejected(x: Double) {
        if (recentRejects.size >= maxRejectsStored) {
            recentRejects.removeAt(0)
        }
        recentRejects.add(x)

        // Apply soft penalty: move mean away from recent rejections
        val penalty = recentRejects.sumOf { reject ->
            var direction = if (reject == maxValue) -2 else if (reject == minValue) 2 else if (reject < mu) 1 else -1

            direction * rejectionWeight
        }
        mu += penalty
        mu = floor(mu).coerceAtMost(maxValue - 1.0).coerceAtLeast(minValue + 1.0)

    }
}