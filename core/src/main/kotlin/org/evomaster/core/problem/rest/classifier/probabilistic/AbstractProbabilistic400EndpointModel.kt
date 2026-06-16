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
 * This class provides:
 * - Common endpoint-specific state (endpoint, warmup, modelKeys, dimension, encoderType, randomness, initialized flag)
 * - A fixed feature-space definition via `modelKeys`
 * - Shared initialization logic
 * - Common performance tracking and metric estimation
 *
 * The classifier assumes a fixed input representation. Each feature is uniquely identified
 * (by e.g., a parameter path) and stored in `modelKeys`, such that:
 *
 *     modelKeys[i] <-> inputVector[i]
 *
 * where `inputVector` is the encoded representation of a `RestCallAction` produced by [InputEncoderUtilWrapper].
 * The ordering of `modelKeys` is important and must remain stable
 * to provide a fixed-length `inputVector` throughout the model's lifetime.
 * This guarantees that the same parameter
 * is always encoded into the same feature dimension.
 */
abstract class AbstractProbabilistic400EndpointModel(
    val endpoint: Endpoint,
    var warmup: Int,
    /**
     * Ordered list of model keys in correspondence with the encoded features i.e.,
     *
     *          modelKeys[i] <-> inputVector[i]
     */
    var modelKeys: List<String>? = null,
    /**
     * Represents the dimension of the feature space (i.e., the length of the input vector)
     */
    var dimension: Int? = null,
    val encoderType: EMConfig.EncoderType,
    val metricType: EMConfig.AIClassificationMetrics,
    val randomness: Randomness
) : AIModel {

    companion object {

        private val log = LoggerFactory.getLogger(AbstractProbabilistic400EndpointModel::class.java)

        const val NOT_400 = 200
    }

    protected var initialized: Boolean = false

    /** Create a metric tracker.*/
    val modelMetrics: ModelMetrics = createModelMetrics(metricType)

    /** Ensure the endpoint matches this model */
    protected fun verifyEndpoint(inputEndpoint: Endpoint) {
        if (inputEndpoint != endpoint) {
            throw IllegalArgumentException("Input endpoint $inputEndpoint does not match model endpoint $endpoint")
        }
    }


    /**
     * Initialize the model's feature space (including the dimension and modelKeys) if it has not been initialized yet.
     * The dimension is the number of parameters in the input vector.
     * The modelKeys are unique identifiers of the parameters defined based on
     * the unique path of each parameter (including all its parents).
     */
    open fun initializeIfNeeded(input: RestCallAction) {

        if (initialized) {
            // already initialized. nothing to do
            return
        }

        val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)
        val allParamsPathsAndEncodedValues = encoder.getAllParamsPathsAndEncodedValues()

        val paramPaths = allParamsPathsAndEncodedValues.keys.toList()

        // It is sufficient to check the parameter paths. The encoder guarantees
        // that every parameter path has a corresponding non-null encoded value.
        require(paramPaths.isNotEmpty()) { "Parameter paths are empty" }
        require(warmup > 0) { "Warmup must be positive" }

        dimension = paramPaths.size
        modelKeys = paramPaths

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

        val keysAndValues = encoder.getAllParamsPathsAndEncodedValues()

        // TODO: initializedKeys are based on the model keys that are fixed.
        //  In fact we ignore new hidden genes added during the search which are not available in the schema.
        //  A more principled solution would be to support dynamic feature spaces,
        //  where the classifier can adapt its dimension and feature mapping as
        //  new parameter structures are discovered during the search.
        val initializedKeys = requireNotNull(modelKeys) {
            "Model keys have not been initialized"
        }

        return initializedKeys.map { key ->

            val value = keysAndValues[key]

            /**
             * The encoder never returns null values.
             * Therefore, a null means that the encoder provided a new key which is not identical to the model key.
             * Hypothetically, this only happens if the parameter path (representing the key)
             * changes during the search due to adding an intermediate gene (e.g., the path for
             * the parameter b changes from 'GET:/a/b' to 'GET:/a/foo/b').
             * Thus, the encoder returns a different key for the parameter from what is expected.
             * In such cases we consider the encoded value as neutral (i.e., 0.0) to avoid errors.
             */
            if (value == null) {
                log.warn("Missing key while encoding endpoint: {}", endpoint)
                log.warn("Model keys: {}", initializedKeys)
                log.warn("Current keys: {}", keysAndValues.keys)
                log.warn("Missing key: {}", key)

                0.0
            } else {
                value
            }

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
