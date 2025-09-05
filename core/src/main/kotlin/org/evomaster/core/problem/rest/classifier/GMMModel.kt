package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

/**
 * Gaussian Mixture Model (GMM) classifier for REST API calls.
 *
 * - Two class-conditional GMMs: one for 2xx and one for 4xx.
 * - Diagonal covariances, K components per class.
 * - Online EM-style update: responsibilities drive weighted incremental updates of means/variances.
 * - Class priors are inferred online from total effective counts in each class model.
 *
 * @param dimension the fixed dimensionality of the input feature vectors.
 * @param warmup the number of warmup updates to familiarize the classifier with at least a few true observations
 */
class GMMModel : AbstractAIModel() {

    var density200: GMM? = null
    var density400: GMM? = null

    /** number of mixture components per class */
    var kComponents: Int = 3

    /** Must be called once to initialize the model properties */
    fun setup(dimension: Int, warmup: Int, kComponents: Int = 3) {
        require(dimension > 0 ) { "Dimension must be positive." }
        require(warmup > 0 ) { "Warmup must be positive." }
        require(kComponents > 0) { "kComponents must be positive." }
        this.dimension = dimension
        this.density200 = GMM(dimension, kComponents)
        this.density400 = GMM(dimension, kComponents)
        this.warmup = warmup
        this.kComponents = kComponents
    }

    override fun classify(input: RestCallAction): AIResponseClassification {

        if (performance.totalSentRequests < warmup) {
            throw IllegalStateException("Classifier not ready as warmup is not completed.")
        }

        val encoder = InputEncoderUtils(input, encoderType = encoderType)
        val inputVector = encoder.encode()
        val d = this.dimension ?: error("Model not setup")

        if (inputVector.size != d) {
            throw IllegalArgumentException("Expected input vector of size $d but got ${inputVector.size}")
        }

        val ll200 = ln(density200!!.weight()) + density200!!.logLikelihood(inputVector)
        val ll400 = ln(density400!!.weight()) + density400!!.logLikelihood(inputVector)

        // work in log-space then exponentiate normalized posteriors
        val m = max(ll200, ll400)
        val p200 = exp(ll200 - m)
        val p400 = exp(ll400 - m)
        val z = p200 + p400
        val posterior200 = p200 / z
        val posterior400 = p400 / z

        return AIResponseClassification(
            probabilities = mapOf(
                200 to posterior200,
                400 to posterior400
            )
        )
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val encoder = InputEncoderUtils(input, encoderType = encoderType)
        val inputVector = encoder.encode()
        val d = this.dimension ?: error("Model not setup")

        if (inputVector.size != d) {
            throw IllegalArgumentException("Expected input vector of size $d but got ${inputVector.size}")
        }

        // Update performance (prediction) before learning the true label
        val trueStatusCode = output.getStatusCode()
        if (performance.totalSentRequests < warmup) {
            performance.updatePerformance(Random.nextBoolean())
        } else {
            val predicted = classify(input).prediction()
            performance.updatePerformance(predicted == trueStatusCode)
        }

        // Update the corresponding class-conditional GMM with online EM step
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
        return this.performance.estimateAccuracy()
    }

    // GMM (diagonal covariance)
    class GMM(private val dimension: Int, private val k: Int) {

        private val comps: Array<Component> = Array(k) { Component(dimension) }
        private var seeded = 0   // number of seeds assigned (for first k observations)
        private val epsVar = 1e-6

        /** Total effective count across components; also drives class prior weight */
        fun weight(): Double = comps.sumOf { it.N }

        /**
         * Online EM-style update for a single sample x:
         * 1) If still seeding, place x as the mean of the next empty component.
         * 2) Otherwise, compute responsibilities with current params.
         * 3) Update each component using weighted Welford updates with r_k.
         */
        fun update(x: List<Double>) {
            require(x.size == dimension)

            if (seeded < k) {
                comps[seeded].seedWith(x)
                seeded++
                return
            }

            // E-step: responsibilities r_k ∝ pi_k * N(x | mu_k, diag sigma_k^2)
            val logResp = DoubleArray(k) { ci ->
                val c = comps[ci]
                ln(c.mixtureWeight(weight())) + logDiagGaussian(x, c.mean, c.variance(epsVar))
            }
            val r = softmaxLog(logResp)

            // M-step (online, single-sample): weighted Welford per component
            for (i in 0 until k) {
                comps[i].weightedUpdate(x, r[i])
            }
        }

        /** log-likelihood under the mixture */
        fun logLikelihood(x: List<Double>): Double {
            require(x.size == dimension)
            if (seeded == 0) {
                // Nothing learned yet -> very low likelihood
                return -1e6
            }
            val logs = DoubleArray(k) { i ->
                val c = comps[i]
                ln(c.mixtureWeight(weight())) + logDiagGaussian(x, c.mean, c.variance(epsVar))
            }
            return logSumExp(logs)
        }

        // ---- helpers ----

        private fun logDiagGaussian(x: List<Double>, mu: List<Double>, varDiag: List<Double>): Double {
            var s = 0.0
            for (i in x.indices) {
                val v = max(varDiag[i], epsVar)
                val diff = x[i] - mu[i]
                s += -0.5 * ln(2 * PI * v) - (diff * diff) / (2 * v)
            }
            return s
        }

        private fun logSumExp(arr: DoubleArray): Double {
            var m = Double.NEGATIVE_INFINITY
            for (v in arr) if (v > m) m = v
            var s = 0.0
            for (v in arr) s += exp(v - m)
            return m + ln(s)
        }

        private fun softmaxLog(logArr: DoubleArray): DoubleArray {
            val m = logArr.maxOrNull() ?: 0.0
            var sum = 0.0
            for (i in logArr.indices) sum += exp(logArr[i] - m)
            val out = DoubleArray(logArr.size)
            for (i in logArr.indices) out[i] = exp(logArr[i] - m) / sum
            return out
        }

        // Mixture component
        class Component(d: Int) {
            // Effective sample count for this component
            var N: Double = 0.0
            val mean: MutableList<Double> = MutableList(d) { 0.0 }
            private val M2: MutableList<Double> = MutableList(d) { 1.0 } // start with prior 1.0 to avoid zero variance

            fun seedWith(x: List<Double>) {
                N = 1.0
                for (i in x.indices) {
                    mean[i] = x[i]
                    M2[i] = 1.0 // keep same stabilizer
                }
            }

            /**
             * Weighted Welford update with responsibility r in [0,1]:
             * N' = N + r
             * mean' = mean + (r / N') * (x - mean)
             * M2' = M2 + r * (x - mean_old) * (x - mean_new)
             */
            fun weightedUpdate(x: List<Double>, r: Double) {
                if (r <= 0.0) return
                val Nnew = N + r
                for (i in x.indices) {
                    val delta = x[i] - mean[i]
                    val incr = (r / Nnew) * delta
                    val meanOld = mean[i]
                    val meanNew = meanOld + incr
                    // M2 uses delta between x and both old/new means
                    M2[i] += r * (x[i] - meanOld) * (x[i] - meanNew)
                    mean[i] = meanNew
                }
                N = Nnew
            }

            fun variance(eps: Double): List<Double> {
                // population-style variance: M2 / N (falls back to 1.0 if tiny)
                return M2.map { v -> if (N > 1e-9) v / N else 1.0 + eps }
            }

            fun mixtureWeight(totalN: Double): Double {
                val denom = if (totalN <= 0.0) 1.0 else totalN
                // small prior to avoid zero
                return (N + 1e-9) / (denom + 1e-9 *  kFakeTotal())
            }

            // a tiny function to keep prior scaling consistent
            private fun kFakeTotal(): Double = 1.0
        }
    }
}
