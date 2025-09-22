package org.evomaster.core.problem.rest.classifier.probabilistic.kde

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Kernel Density Estimation (KDE) classifier for REST API calls.
 *
 * - Two class-conditional KDEs: one for 400 and one for not 400.
 * - Product Gaussian kernels with diagonal bandwidths per dimension.
 * - Online updates: we store samples and maintain running mean/variance to compute bandwidths.
 * - Class priors inferred from sample counts in each class.
 */
class KDE400EndpointModel (
    endpoint: Endpoint,
    warmup: Int = 10,
    dimension: Int? = null,
    encoderType: EMConfig.EncoderType= EMConfig.EncoderType.NORMAL,
    randomness: Randomness
): AbstractProbabilistic400EndpointModel(endpoint, warmup, dimension, encoderType, randomness) {

    private var density400: KDE? = null
    private var densityNot400: KDE? = null

    /**
     * Optional cap on stored samples per class (0 = unlimited).
     * If >0, uses reservoir-style uniform downsampling.
     */
    var maxSamplesPerClass: Int = 0

    /** Must be called once to initialize the model properties */
    override fun initializeIfNeeded(inputVector: List<Double>) {
        super.initializeIfNeeded(inputVector)
        if(density400 == null) {
            density400 = KDE(dimension!!, maxSamplesPerClass)
        }
        if(densityNot400 == null) {
            densityNot400 = KDE(dimension!!, maxSamplesPerClass)
        }
        initialized = true
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

        val ll400 = ln((density400!!.weight()).coerceAtLeast(1.0)) + density400!!.logLikelihood(inputVector)
        val llNot400 = ln((densityNot400!!.weight()).coerceAtLeast(1.0)) + densityNot400!!.logLikelihood(inputVector)

        // log-space normalization
        val m = max(llNot400, ll400)
        val likelihood400 = exp(ll400 - m)
        val likelihoodNot400 = exp(llNot400 - m)

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
         * Updating the KDEs based on the real observation
         */
        if (trueStatusCode == 400) {
            density400!!.add(inputVector)
        } else {
            densityNot400!!.add(inputVector)
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