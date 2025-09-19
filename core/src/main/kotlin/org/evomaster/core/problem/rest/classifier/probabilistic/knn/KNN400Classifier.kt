package org.evomaster.core.problem.rest.classifier.probabilistic.knn

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class KNN400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    private val k: Int = 3,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<KNN400EndpointModel>(warmup, encoderType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        randomness: Randomness
    ): KNN400EndpointModel {
        return KNN400EndpointModel(
            endpoint,
            warmup,
            dimension,
            encoderType,
            k = k,
            randomness
        )
    }
}