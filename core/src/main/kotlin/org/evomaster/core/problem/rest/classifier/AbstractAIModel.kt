package org.evomaster.core.problem.rest.classifier

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.Endpoint

abstract class AbstractAIModel(): AIModel {

    var warmup: Int = 10
    var dimension: Int? = null
    var encoderType: EMConfig.EncoderType?= null
    val performance = ModelAccuracyFullHistory()
    val modelAccuracy: ModelAccuracy = ModelAccuracyWithTimeWindow(20)

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return performance.estimateAccuracy()
    }

    override fun estimateOverallAccuracy(): Double {
        return modelAccuracy.estimateAccuracy()
    }

}