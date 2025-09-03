package org.evomaster.core.problem.rest.classifier.deterministic

import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult

class Deterministic400Classifier(
    private val thresholdForClassification : Double = 0.8
) : AIModel {

    private val models: MutableMap<Endpoint, Deterministic400EndpointModel> = mutableMapOf()

    override fun updateModel(
        input: RestCallAction,
        output: RestCallResult
    ) {
        val m = models.getOrPut(input.endpoint) {
            Deterministic400EndpointModel(input.endpoint, thresholdForClassification)
        }
        m.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        val m = models[input.endpoint] ?:
            return  AIResponseClassification()

        return m.classify(input)
    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        val m = models[endpoint] ?:
            return 0.0

        return m.estimateAccuracy(endpoint)
    }
}
