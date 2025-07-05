package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.GLMOnlineClassifier
import org.evomaster.core.problem.rest.classifier.GaussianOnlineClassifier
import javax.annotation.PostConstruct



class AIResponseClassifier : AIModel {

    @Inject
    private lateinit var config: EMConfig

    private lateinit var delegate: AIModel

    @PostConstruct
    fun initModel() {
        delegate = when (config.aiModelForResponseClassification) {
            EMConfig.AIResponseClassifierModel.GAUSSIAN ->
                GaussianOnlineClassifier()
            EMConfig.AIResponseClassifierModel.GLM ->
                GLMOnlineClassifier(config.aiResponseClassifierLearningRate)
            else -> object : AIModel {
                override fun updateModel(input: RestCallAction, output: RestCallResult) {}
                override fun classify(input: RestCallAction) = AIResponseClassification()
            }
        }
    }


    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        delegate.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        return delegate.classify(input)
    }

    fun viewInnerModel(): AIModel = delegate

    /**
     * If the model thinks this call will lead to a user error (eg 400), then try to repair
     * the action to be able to solve the input constraints, aiming for a 2xx.
     * There is no guarantee that this will work.
     */
    fun attemptRepair(reference: RestCallAction){

        //TODO
    }
}
