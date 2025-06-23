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



class AIResponseClassifier(
    private val config: EMConfig = EMConfig(),
    private val explicitModel: AIModel? = null
) : AIModel {

    private val model: AIModel = explicitModel ?: when (config.aiModelForResponseClassification) {
        EMConfig.AIResponseClassifierModel.GAUSSIAN -> GaussianOnlineClassifier()
        EMConfig.AIResponseClassifierModel.GLM -> GLMOnlineClassifier(config.aiResponseClassifierLearningRate)
        else -> object : AIModel {
            override fun updateModel(input: RestCallAction, output: RestCallResult) {}
            override fun classify(input: RestCallAction) = AIResponseClassification()
        }
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        model.updateModel(input, output)
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        return model.classify(input)
    }

    fun getInnerModel(): AIModel = model

    /**
     * If the model thinks this call will lead to a user error (eg 400), then try to repair
     * the action to be able to solve the input constraints, aiming for a 2xx.
     * There is no guarantee that this will work.
     */
    fun attemptRepair(reference: RestCallAction){

        //TODO
    }
}
