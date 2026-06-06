package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.quantifier.ModelEvaluation
import org.evomaster.core.problem.rest.classifier.quantifier.ModelMetrics
import org.evomaster.core.problem.rest.classifier.quantifier.createModelMetrics
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness
import org.slf4j.LoggerFactory

/**
 * Base class for all probabilistic classifiers working at an endpoint (400 vs. not 400).
 *
 * Provides:
 * - Common properties (endpoint, warmup, keys, dimension, encoderType, randomness, initialized flag)
 * - Shared methods for initialization checks and accuracy estimation
 */
abstract class AbstractProbabilistic400EndpointModel(
    val endpoint: Endpoint,
    var warmup: Int,
    var modelKeys: List<String>? = null,
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType,
    val metricType: EMConfig.AIClassificationMetrics,
    val randomness: Randomness
) : AIModel {

    protected var initialized: Boolean = false

    companion object {

        private val log = LoggerFactory.getLogger(AbstractProbabilistic400EndpointModel::class.java)

        const val NOT_400 = 200
    }

    /** Create a metric tracker.*/
    val modelMetrics: ModelMetrics = createModelMetrics(metricType)

    /** Ensure the endpoint matches this model */
    protected fun verifyEndpoint(inputEndpoint: Endpoint) {
        if (inputEndpoint != endpoint) {
            throw IllegalArgumentException("Input endpoint $inputEndpoint does not match model endpoint $endpoint")
        }
    }


    /**
     * Initialize dimension and keys once initialized.
     * The dimension is the number of parameters in the input vector.
     * The modelKeys are unique identifiers of the parameters defined based on
     * the unique path of each parameter (including all its parents).
     */
    open fun initializeIfNeeded(input: RestCallAction) {

        if (dimension == null) {

            val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
            val allParamsPathsAndEncodedValues = encoder.getAllParamsPathsAndEncodedValues()

            val inputVector = allParamsPathsAndEncodedValues.values.toList()
            val paramPaths = allParamsPathsAndEncodedValues.keys.toList()

            require(inputVector.isNotEmpty()) { "Input vector is empty" }
            require(paramPaths.isNotEmpty()) { "Parameter paths are empty" }
            require(warmup > 0) { "Warmup must be positive" }

            dimension = paramPaths.size
            modelKeys = paramPaths

        }

        initialized = true
    }

    /**
     * Updating classifier performance based on its prediction
     * Before the warmup is completed, the update is based on a crude guess (like a coin flip).
     */
    protected fun updateModelMetrics(action: RestCallAction, result: RestCallResult) {

        val outputStatusCode= result.getStatusCode()
        if (modelMetrics.totalSentRequests < warmup || action.parameters.isEmpty()) {
            val predictedStatusCode = if(randomness.nextBoolean()) 400 else NOT_400
            modelMetrics.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
        } else {
            val predictedStatusCode = classify(action).prediction()
            modelMetrics.updatePerformance(predictedStatusCode, outputStatusCode?:-1)
        }

    }

    /**
     * Encodes the input parameters using the model's defined keys and returns the corresponding encoded values.
     * This method ensures that all expected keys (modelKeys) are present in the encoded input.
     *
     * @param input The input action containing required data for parameter encoding.
     * @return A list of encoded values corresponding to the model's keys.
     */
    protected fun encodeUsingModelKeys(
        input: RestCallAction
    ): List<Double> {

        val encoder = InputEncoderUtilWrapper(
            input,
            encoderType = encoderType
        )

        val keysAndValues =
            encoder.getAllParamsPathsAndEncodedValues()

        val initializedKeys = requireNotNull(modelKeys) {
            "Model keys have not been initialized"
        }

        return initializedKeys.map { key ->

            val value = keysAndValues[key]

            if (value == null) {

                log.error("Endpoint: {}", endpoint)
                log.error("Missing expected key: {}", key)
                log.error("Model keys: {}", initializedKeys)
                log.error("Current keys: {}", keysAndValues.keys)

                val missing = initializedKeys.filter { it !in keysAndValues.keys }
                log.error("Missing keys: {}", missing)

                throw IllegalArgumentException(
                    "The encoded value cannot be null as the encoder set nulls to the sentinel (e.g., 10^6)." +
                            " So there is a missing key as: $key"
                )
            }

            value
        }
    }

    /** Default metrics estimates */
    override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation {
        verifyEndpoint(endpoint)
        return estimateOverallMetrics()
    }

    /** Default overall metrics estimates */
    override fun estimateOverallMetrics(): ModelEvaluation {
        if (!initialized) {
            // hasn’t learned anything yet
            return ModelEvaluation.DEFAULT_NO_DATA
        }
        // This is a single-endpoint model and just return its own metrics
        return modelMetrics.estimateMetrics()
    }

}
