package org.evomaster.core.problem.rest.classifier.probabilistic.glm

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class GLM400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    private val learningRate: Double = 0.01,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<GLM400EndpointModel>(warmup, encoderType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        randomness: Randomness
    ): GLM400EndpointModel {
        return GLM400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            learningRate,
            randomness
        )
    }
}