package org.evomaster.core.problem.rest.classifier.probabilistic.dnn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness

class DNN400Classifier(
    warmup: Int,
    encoderType: EMConfig.EncoderType,
    metricType: EMConfig.AIClassificationMetrics,
    randomness: Randomness,
    private val learningRate: Double = 0.001,
    private val maxStoredSamples: Int = 1024
) : AbstractProbabilistic400Classifier<DNN400EndpointModel>(warmup, encoderType, metricType, randomness) {
    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        modelKeys: List<String>,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ) = DNN400EndpointModel(
        endpoint, warmup, modelKeys, dimension, encoderType, metricType,
        learningRate, randomness, maxStoredSamples = maxStoredSamples
    )
}
