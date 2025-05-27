package org.evomaster.core.problem.rest.aiconstraint.numeric

import kotlin.math.*
import kotlin.random.Random

class NaiveGaussianModel1D(
    initialMean: Int = 0,
    initialVariance: Double = 1.0,
    private val minValue: Int = Int.MIN_VALUE,
    private val maxValue: Int = Int.MAX_VALUE,
) {
    private var n = 1
    private var mu = initialMean.toDouble()
    private var M2 = initialVariance
    private val rng = Random.Default

    // Rejection tracking
    private val recentRejects = mutableListOf<Int>()
    private val maxRejectsStored = 20
    private val rejectionWeight = 0.1  // how strongly to move away

    fun updateAccepted(x: Int) {
        val dx = x.toDouble()
        n += 1
        val delta = dx - mu
        mu += delta / n
        M2 += delta * (dx - mu)

        mu = floor(mu).coerceAtMost(maxValue - 1.0).coerceAtLeast(minValue + 1.0)
    }

    fun updateRejected(x: Int) {
        if (recentRejects.size >= maxRejectsStored) {
            recentRejects.removeAt(0)
        }
        recentRejects.add(x)

        // Apply soft penalty: move mean away from recent rejections
        val penalty = recentRejects.sumOf { reject ->
            var direction = if (reject == maxValue) -2 else if (reject == minValue) 2 else if (reject < mu) 1 else -1

//            println("reject: $reject, mu: $mu direction: $direction")
            direction * rejectionWeight
        }
        mu += penalty
        mu = floor(mu).coerceAtMost(maxValue - 1.0).coerceAtLeast(minValue + 1.0)

    }

    fun mean(): Double = mu
    fun variance(): Double = if (n > 1) M2 / (n - 1) else M2

    fun sample(): Int {
        val stdDev = sqrt(variance().coerceAtLeast(1e-3))
        val raw = rng.nextGaussian(mu, stdDev)
        val rounded = raw.roundToInt()
        return rounded.coerceIn(minValue, maxValue)
    }

    private fun Random.nextGaussian(mean: Double, stdDev: Double): Double {
        val u1 = nextDouble()
        val u2 = nextDouble()
        val z0 = sqrt(-2.0 * ln(u1)) * cos(2 * Math.PI * u2)
        return z0 * stdDev + mean
    }
}
