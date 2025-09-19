package org.evomaster.core.problem.rest.classifier.probabilistic.gaussian

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier


class Gaussian400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<Gaussian400EndpointModel>(warmup, encoderType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        randomness: Randomness
    ): Gaussian400EndpointModel {
        return Gaussian400EndpointModel(endpoint, warmup, dimension, encoderType, randomness)
    }
}
