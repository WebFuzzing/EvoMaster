package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import kotlin.math.*

/**
 * Kernel Density Estimation (KDE) classifier for REST API calls.
 *
 * - Two class-conditional KDEs: one for 2xx and one for 4xx.
 * - Product Gaussian kernels with diagonal bandwidths per dimension.
 * - Online updates: we store samples and maintain running mean/variance to compute bandwidths.
 * - Class priors inferred from sample counts in each class.
 */
class KDEModel : AbstractAIModel() {

    var density200: KDE? = null
    var density400: KDE? = null

    /**
     * Optional cap on stored samples per class (0 = unlimited).
     * If >0, uses reservoir-style uniform downsampling.
     */
    var maxSamplesPerClass: Int = 0

    /** Must be called once to initialize the model properties */
    fun setup(dimension: Int, warmup: Int, maxSamplesPerClass: Int = 0) {
        require(dimension > 0) { "Dimension must be positive." }
        require(warmup > 0) { "Warmup must be positive." }
        require(maxSamplesPerClass >= 0) { "maxSamplesPerClass must be >= 0" }
        this.dimension = dimension
        this.density200 = KDE(dimension, maxSamplesPerClass)
        this.density400 = KDE(dimension, maxSamplesPerClass)
        this.warmup = warmup
        this.maxSamplesPerClass = maxSamplesPerClass
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        if (performance.totalSentRequests < warmup) {
            throw IllegalStateException("Classifier not ready as warmup is not completed.")
        }

        val d = this.dimension ?: error("Model not setup")
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()
        require(inputVector.size == d) { "Expected input vector of size $d but got ${inputVector.size}" }

        val ll200 = ln((density200!!.weight()).coerceAtLeast(1.0)) + density200!!.logLikelihood(inputVector)
        val ll400 = ln((density400!!.weight()).coerceAtLeast(1.0)) + density400!!.logLikelihood(inputVector)

        // log-space normalization
        val m = max(ll200, ll400)
        val p200 = exp(ll200 - m)
        val p400 = exp(ll400 - m)
        val z = p200 + p400
        val post200 = p200 / z
        val post400 = p400 / z

        return AIResponseClassification(
            probabilities = mapOf(200 to post200, 400 to post400)
        )
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val d = this.dimension ?: error("Model not setup")
        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val inputVector = encoder.encode()
        require(inputVector.size == d) { "Expected input vector of size $d but got ${inputVector.size}" }

        // Update performance estimate (prediction made before seeing the label)
        val trueStatusCode = output.getStatusCode()
        if (performance.totalSentRequests < warmup) {
            performance.updatePerformance(kotlin.random.Random.nextBoolean())
        } else {
            val predicted = classify(input).prediction()
            performance.updatePerformance(predicted == trueStatusCode)
        }

        // Update class-specific KDE with the true observation
        if (trueStatusCode == 400) {
            density400!!.add(inputVector)
        } else {
            density200!!.add(inputVector)
        }


    }

    /**
     * Represents a Kernel Density Estimator (KDE) that approximates the distribution of a dataset
     * using Gaussian kernels with diagonal bandwidth.
     * @property d Dimensionality of the data points.
     * @property maxStored Maximum number of samples to store in memory. If the value is <= 0, all samples are stored.
     */
    class KDE(private val d: Int, private val maxStored: Int = 0) {

        private val samples = mutableListOf<DoubleArray>()
        private var seen: Long = 0L // total seen (for reservoir)
        private val mean = DoubleArray(d) { 0.0 }
        private val M2 = DoubleArray(d) { 1.0 } // stabilizer to avoid zero variance initially
        private val epsVar = 1e-6
        private val epsBW = 1e-3

        fun weight(): Double = samples.size.toDouble()

        fun add(xList: List<Double>) {
            require(xList.size == d)
            val x = xList.toDoubleArray()
            seen++

            // Update running moments for bandwidth
            for (j in 0 until d) {
                val delta = x[j] - mean[j]
                mean[j] += delta / seen
                val delta2 = x[j] - mean[j]
                M2[j] += delta * delta2
            }

            // Store sample (unbounded or reservoir downsample)
            if (maxStored <= 0) {
                samples.add(x)
            } else {
                if (samples.size < maxStored) {
                    samples.add(x)
                } else {
                    // reservoir: replace with decreasing probability
                    val r = kotlin.random.Random.nextLong(seen)
                    if (r < maxStored) {
                        samples[r.toInt()] = x
                    }
                }
            }
        }

        /** Per-dimension variance estimate */
        private fun variances(): DoubleArray {
            val n = max(1L, seen)
            return DoubleArray(d) { j ->
                val v = M2[j] / n.toDouble()
                if (v.isFinite() && v > epsVar) v else 1.0
            }
        }

        /** Scott's rule: h_j = sigma_j * n^(-1/(d+4)), with floors */
        private fun bandwidths(nEff: Int, varDiag: DoubleArray): DoubleArray {
            val n = max(nEff, 1)
            val power = -1.0 / (d + 4.0)
            val nFactor = n.toDouble().pow(power)
            return DoubleArray(d) { j ->
                val sj = sqrt(varDiag[j]).coerceAtLeast(sqrt(epsVar))
                (sj * nFactor).coerceAtLeast(epsBW)
            }
        }

        /** log p(x) = log( (1/n) Σ_i N(x | s_i, diag(h^2)) ) */
        fun logLikelihood(xList: List<Double>): Double {
            require(xList.size == d)
            val n = samples.size
            if (n == 0) return -1e6

            val varDiag = variances()
            val h = bandwidths(n, varDiag)
            val logNorm = -0.5 * d * ln(2 * Math.PI) - h.sumOf { ln(it) } // since diag(h^2): |H|^(1/2) = Π h_j

            // For each sample, compute log kernel: log φ_h(x - s_i)
            val logs = DoubleArray(n)
            for (i in 0 until n) {
                val s = samples[i]
                var quad = 0.0
                for (j in 0 until d) {
                    val z = (xList[j] - s[j]) / h[j]
                    quad += z * z
                }
                logs[i] = logNorm - 0.5 * quad
            }

            // log-mean-exp
            var m = Double.NEGATIVE_INFINITY
            for (v in logs) if (v > m) m = v
            var sumExp = 0.0
            for (v in logs) sumExp += exp(v - m)
            return m + ln(sumExp) - ln(n.toDouble())
        }
    }

}
