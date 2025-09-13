package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.problem.rest.data.Endpoint

abstract class AbstractAIModel: AIModel {
    val performance = ModelAccuracyFullHistory()
    var warmup: Int = 10
    var dimension: Int? = null
    var encoderType: EncoderType = EncoderType.NORMAL

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return performance.estimateAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        return performance.estimateAccuracy()
    }

}