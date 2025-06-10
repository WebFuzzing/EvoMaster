package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.GaussianOnlineClassifier
import javax.annotation.PostConstruct


class AIResponseClassifier : AIModel {

    @Inject
    private lateinit var config : EMConfig

    private lateinit var delegate: AIModel

    fun initModel(dimension: Int){

        when(config.aiModelForResponseClassification){
            EMConfig.AIResponseClassifierModel.GAUSSIAN -> {
                //TODO
                delegate = GaussianOnlineClassifier(dimension)
            }
            EMConfig.AIResponseClassifierModel.NN -> {
                //TODO
            }
            EMConfig.AIResponseClassifierModel.NONE -> {
                //TODO
                delegate = object : AIModel {
                    override fun updateModel(input: RestCallAction, output: RestCallResult) {}
                    override fun classify(input: RestCallAction) = AIResponseClassification()
                }
            }
        }
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        delegate.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        return delegate.classify(input)
    }

}

