package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.ModelEvaluation
import org.evomaster.core.problem.rest.classifier.ModelMetrics
import org.evomaster.core.problem.rest.classifier.ModelMetricsFullHistory
import org.evomaster.core.problem.rest.classifier.ModelMetricsWithTimeWindow
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness

/**
 * Base class for all probabilistic classifiers working at an endpoint (400 vs. not 400).
 *
 * Provides:
 * - Common properties (endpoint, warmup, encoderType, randomness, dimension, initialized flag)
 * - Shared methods for initialization checks and accuracy estimation
 */
abstract class AbstractProbabilistic400EndpointModel(
    val endpoint: Endpoint,
    var warmup: Int = 10,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    val randomness: Randomness
) : AIModel {

    protected var initialized: Boolean = false

    val modelMetricsFullHistory: ModelMetricsFullHistory = ModelMetricsFullHistory()
    val modelMetrics: ModelMetrics = ModelMetricsWithTimeWindow(20)

    /** Ensure endpoint matches this model */
    protected fun verifyEndpoint(inputEndpoint: Endpoint) {
        if (inputEndpoint != endpoint) {
            throw IllegalArgumentException("Input endpoint $inputEndpoint does not match model endpoint $endpoint")
        }
    }

    /** Initialize dimension once, validate consistency */
    open fun initializeIfNeeded(inputVector: List<Double>) {
        if (dimension == null) {
            require(inputVector.isNotEmpty()) { "Input vector cannot be empty" }
            require(warmup > 0) { "Warmup must be positive" }
            dimension = inputVector.size
        } else {
            require(inputVector.size == dimension) {
                "Expected input vector of size $dimension but got ${inputVector.size}"
            }
        }
        initialized = true
    }

    /**
     * Updating classifier performance based on its prediction
     * Before the warmup is completed, the update is based on a crude guess (like a coin flip).
     */
    protected fun updateModelMetrics(action: RestCallAction, result: RestCallResult) {

        val outputStatusCode= result.getStatusCode()
        if (modelMetricsFullHistory.totalSentRequests < warmup || action.parameters.isEmpty()) {
            val predictedStatusCode = if(randomness.nextBoolean()) 400 else 200
            modelMetricsFullHistory.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
            modelMetrics.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
        } else {
            val predictedStatusCode = classify(action).prediction()
            modelMetricsFullHistory.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
            modelMetrics.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
        }

    }

    /** Default metrics estimates */
    override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation {
        verifyEndpoint(endpoint)
        if (!initialized) {
            // hasn’t learned anything yet → return defaults
            return ModelEvaluation(
                accuracy = 0.5,
                precision400 = 0.5,
                recall400 = 0.0,
                f1Score400 = 0.0,
                mcc = 0.0
            )
        }

        val acc = modelMetrics.estimateAccuracy()
        val prec = modelMetrics.estimatePrecision400()
        val rec = modelMetrics.estimateRecall400()
        val f1 = if (prec + rec == 0.0) 0.0 else 2 * (prec * rec) / (prec + rec)
        val mcc = modelMetrics.estimateMCC400()

        return ModelEvaluation(acc, prec, rec, f1, mcc)
    }

    /** Default overall metrics estimates */
    override fun estimateOverallMetrics(): ModelEvaluation {
        if (!initialized) {
            // hasn’t learned anything yet
            return ModelEvaluation(0.5, 0.5, 0.0, 0.0, 0.0)
        }
        return modelMetrics.estimateMetrics()
    }

}
