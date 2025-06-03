package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.NaiveGaussian1DClassifier
import javax.annotation.PostConstruct


class AIResponseClassifier : AIModel {

    @Inject
    private lateinit var config : EMConfig

    private lateinit var delegate: AIModel


    @PostConstruct
    fun initModel(){

        when(config.aiModelForResponseClassification){
            EMConfig.AIResponseClassifierModel.GAUSSIAN -> {
                //TODO
            }
            EMConfig.AIResponseClassifierModel.NN -> {
                //TODO
            }
            EMConfig.AIResponseClassifierModel.NONE -> {
                //TODO
            }
            EMConfig.AIResponseClassifierModel.NAIVE_GAUSSIAN_1D -> {
                delegate = NaiveGaussian1DClassifier()
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

