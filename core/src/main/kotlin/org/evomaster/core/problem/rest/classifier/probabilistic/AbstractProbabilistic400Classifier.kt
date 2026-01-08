package org.evomaster.core.problem.rest.classifier.probabilistic

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.quantifier.ModelEvaluation
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.service.Randomness

/**
 * Abstract base class for all 400 probabilistic classifiers (Gaussian, GLM, KDE, KNN, NN).
 *
 * Handles:
 *  - Map of endpoint → endpoint model
 *  - Unsupported endpoints tracking
 *  - Common implementations of classifying / estimateAccuracy / estimateOverallAccuracy
 *
 */
abstract class AbstractProbabilistic400Classifier<T : AIModel>(
    private val warmup: Int,
    private val encoderType: EMConfig.EncoderType,
    private val metricType: EMConfig.AIClassificationMetrics,
    private val randomness: Randomness
) : AIModel {

    protected val models: MutableMap<Endpoint, T?> = mutableMapOf()
    protected val unsupportedEndpoints: MutableSet<Endpoint> = mutableSetOf()

    fun getModel(endpoint: Endpoint): T? = models[endpoint]
    fun getAllModels(): Map<Endpoint, T?> = models.toMap()

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        val endpoint = input.endpoint

        if (unsupportedEndpoints.contains(endpoint)) {
            return
        }

        val m = models.getOrPut(endpoint) {
            val encoder = InputEncoderUtilWrapper(input, encoderType = encoderType)

            if (encoder.areAllGenesUnSupported()) {
                unsupportedEndpoints.add(endpoint)
                return@getOrPut null
            }

            val listGenes = encoder.endPointToGeneList().map { it.gene.getLeafGene() }
            createEndpointModel(
                endpoint, warmup,
                listGenes.size,
                encoderType,
                metricType,
                randomness)
        }

        if (m == null) {
            unsupportedEndpoints.add(endpoint)
            return
        }

        m.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val m = models[input.endpoint] ?: return AIResponseClassification(probabilities = mapOf(400 to 0.5))
        return m.classify(input)
    }

    override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation {
        val m = models[endpoint] ?: return ModelEvaluation.DEFAULT_NO_DATA
        return m.estimateMetrics(endpoint)
    }

    override fun estimateOverallMetrics(): ModelEvaluation {
        if (models.isEmpty()) {
            return ModelEvaluation.DEFAULT_NO_DATA
        }

        val n = models.size.toDouble()
        val total = models.values.map {
            it?.estimateOverallMetrics() ?: ModelEvaluation.DEFAULT_NO_DATA
        }

        return ModelEvaluation(
            accuracy = total.sumOf { it.accuracy } / n,
            precision400 = total.sumOf { it.precision400 } / n,
            recall400 = total.sumOf { it.recall400 } / n,
            mcc = total.sumOf { it.mcc } / n
        )
    }

    /**
     * Subclasses must implement how to create a concrete endpoint model.
     */
    protected abstract fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ): T
}