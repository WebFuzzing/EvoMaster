package org.evomaster.core.problem.rest.classifier.probabilistic.kde

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400EndpointModel
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness

class KDE400Classifier(
    warmup: Int = 10,
    encoderType: EMConfig.EncoderType = EMConfig.EncoderType.NORMAL,
    randomness: Randomness
) : AbstractProbabilistic400Classifier<KDE400EndpointModel>(warmup, encoderType, randomness) {

    override fun createEndpointModel(
        endpoint: Endpoint,
        warmup: Int,
        dimension: Int,
        encoderType: EMConfig.EncoderType,
        randomness: Randomness
    ): KDE400EndpointModel {
        return KDE400EndpointModel(endpoint, warmup, dimension, encoderType, randomness)
    }
}
