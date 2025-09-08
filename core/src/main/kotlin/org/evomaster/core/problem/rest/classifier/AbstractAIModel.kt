package org.evomaster.core.problem.rest.classifier

abstract class AbstractAIModel: AIModel {
    val performance = ModelAccuracyFullHistory()
    var warmup: Int = 10
    var dimension: Int? = null
    var encoderType: EncoderType = EncoderType.NORMAL
}