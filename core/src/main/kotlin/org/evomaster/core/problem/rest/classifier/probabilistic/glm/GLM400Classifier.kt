package org.evomaster.core.problem.rest.classifier.probabilistic.glm

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class GLM400Classifier(
    warmup: Int,
    encoderType: EMConfig.EncoderType,
    metricType: EMConfig.AIClassificationMetrics,
    private val learningRate: Double = 0.01,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<GLM400EndpointModel>(
    warmup, encoderType, metricType ,randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ): GLM400EndpointModel {
        return GLM400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            metricType,
            learningRate,
            randomness
        )
    }
}