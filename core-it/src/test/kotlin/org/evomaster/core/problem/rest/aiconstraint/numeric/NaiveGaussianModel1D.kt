package org.evomaster.core.problem.rest.aiconstraint.numeric
import kotlin.math.*
import kotlin.random.Random

class NaiveGaussianModel1D(
    initialMean: Double = 0.0,
    initialVariance: Double = 1.0
) {
    private var n = 1
    private var mu = initialMean
    private var M2 = initialVariance
    private val rng = Random.Default

    // Rejection tracking
    private val recentRejects = mutableListOf<Double>()
    private val maxRejectsStored = 20
    private val rejectionWeight = 0.1  // how strongly to move away

    fun updateAccepted(x: Double) {
        n += 1
        val delta = x - mu
        mu += delta / n
        M2 += delta * (x - mu)
    }

    fun updateRejected(x: Double) {
        if (recentRejects.size >= maxRejectsStored) {
            recentRejects.removeAt(0)
        }
        recentRejects.add(x)

        // Apply soft penalty: move mean away from recent rejections
        val penalty = recentRejects.sumOf { reject ->
            val direction = if (reject < mu) 1 else -1  // push away
            direction * rejectionWeight
        }
        mu += penalty
    }

    fun mean(): Double = mu
    fun variance(): Double = if (n > 1) M2 / (n - 1) else M2

    fun sample(): Double {
        val stdDev = sqrt(variance().coerceAtLeast(1e-3))
        return rng.nextGaussian(mu, stdDev)
    }

    private fun Random.nextGaussian(mean: Double, stdDev: Double): Double {
        val u1 = nextDouble()
        val u2 = nextDouble()
        val z0 = sqrt(-2.0 * ln(u1)) * cos(2 * Math.PI * u2)
        return z0 * stdDev + mean
    }
}
