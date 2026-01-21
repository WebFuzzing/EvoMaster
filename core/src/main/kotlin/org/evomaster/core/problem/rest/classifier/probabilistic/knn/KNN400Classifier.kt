package org.evomaster.core.problem.rest.classifier.probabilistic.knn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class KNN400Classifier(
    warmup: Int,
    encoderType: EMConfig.EncoderType,
    metricType: EMConfig.AIClassificationMetrics,
    private val k: Int = 3,
    private val maxStoredSamples: Int = 10000,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<KNN400EndpointModel>(
    warmup, encoderType, metricType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        metricType: EMConfig.AIClassificationMetrics,
        randomness: Randomness
    ): KNN400EndpointModel {
        return KNN400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            metricType,
            k = k,
            maxStoredSamples = maxStoredSamples,
            randomness
        )
    }
}