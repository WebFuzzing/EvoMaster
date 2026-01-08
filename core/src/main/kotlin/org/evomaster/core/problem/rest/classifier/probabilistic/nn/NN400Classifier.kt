package org.evomaster.core.problem.rest.classifier.probabilistic.nn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class NN400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    metricType: EMConfig.AIClassificationMetrics = EMConfig.AIClassificationMetrics.TIME_WINDOW,
    private val learningRate: Double = 0.01,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<NN400EndpointModel>(
    warmup, encoderType, metricType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ): NN400EndpointModel {
        return NN400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            metricType,
            learningRate= learningRate,
            randomness
        )
    }
}