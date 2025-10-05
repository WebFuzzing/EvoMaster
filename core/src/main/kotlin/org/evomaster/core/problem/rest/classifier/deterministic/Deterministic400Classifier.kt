package org.evomaster.core.problem.rest.classifier.deterministic

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.ModelEvaluation
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

class Deterministic400Classifier(
    private val thresholdForClassification : Double = 0.8,
    private val metricType: EMConfig.AIClassificationMetrics
) : AIModel {

    private val models: MutableMap<Endpoint, Deterministic400EndpointModel> = mutableMapOf()

    override fun updateModel(
        input: RestCallAction,
        output: RestCallResult
    ) {
        val m = models.getOrPut(input.endpoint) {
            Deterministic400EndpointModel(input.endpoint, thresholdForClassification,metricType = metricType)
        }
        m.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val m = models[input.endpoint] ?:
            return  AIResponseClassification()

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

}
