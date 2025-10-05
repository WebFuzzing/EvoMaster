package org.evomaster.core.problem.rest.classifier.probabilistic.kde

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness

class KDE400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    metricType: EMConfig.AIClassificationMetrics = EMConfig.AIClassificationMetrics.TIME_WINDOW,
    private val maxStoredSamples: Int = 10_000,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<KDE400EndpointModel>(
    warmup, encoderType, metricType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ): KDE400EndpointModel {
        return KDE400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            metricType,
            maxStoredSamples = maxStoredSamples,
            randomness)
    }
}
