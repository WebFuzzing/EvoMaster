package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.GLMOnlineClassifier
import org.evomaster.core.problem.rest.classifier.GaussianOnlineClassifier
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.search.service.Randomness
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct



class AIResponseClassifier : AIModel {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AIResponseClassifier::class.java)
    }

    @Inject
    private lateinit var config: EMConfig

    @Inject
    private lateinit var randomness: Randomness

    private lateinit var delegate: AIModel

    private var enabledLearning : Boolean = true

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
                override fun estimateAccuracy(endpoint: Endpoint): Double  = 0.0
            }
        }
    }


    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        if(enabledLearning) {
            delegate.updateModel(input, output)
        } else {
            log.warn("Trying to update model, but learning is disabled. This should ONLY happen when running tests in EM")
        }
    }

    override fun classify(input: RestCallAction): AIResponseClassification {
        return delegate.classify(input)
    }

    override fun estimateAccuracy(endpoint: Endpoint): Double {
        return delegate.estimateAccuracy(endpoint)
    }

    fun viewInnerModel(): AIModel = delegate

    /**
     * If the model thinks this call will lead to a user error (eg 400), then try to repair
     * the action to be able to solve the input constraints, aiming for a 2xx.
     * There is no guarantee that this will work.
     */
    fun attemptRepair(call: RestCallAction){

        val accuracy = estimateAccuracy(call.endpoint)
        //TODO any better way to use this accuracy?
        if(!randomness.nextBoolean(accuracy)){
            //do nothing
            return
        }

        val classification = classify(call)

        if(randomness.nextBoolean(classification.probabilityOf400())){
            //TODO try repair
        }
        //TODO
    }

    /**
     * Only needed during testing, to avoid modifying model while evaluating manually crafted actions
     */
    fun disableLearning(){
        enabledLearning = false
    }
}
