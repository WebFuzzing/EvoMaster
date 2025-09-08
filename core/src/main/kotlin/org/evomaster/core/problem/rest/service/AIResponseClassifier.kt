package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.GLMModel
import org.evomaster.core.problem.rest.classifier.GaussianModel
import org.evomaster.core.problem.rest.classifier.deterministic.Deterministic400Classifier
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
                GaussianModel()
            EMConfig.AIResponseClassifierModel.GLM ->
                GLMModel(config.aiResponseClassifierLearningRate)
            EMConfig.AIResponseClassifierModel.DETERMINISTIC ->
                Deterministic400Classifier(config.classificationRepairThreshold)
            else -> object : AIModel {
                override fun updateModel(input: RestCallAction, output: RestCallResult) {}
                override fun classify(input: RestCallAction) = AIResponseClassification()
                override fun estimateAccuracy(endpoint: Endpoint): Double  = 0.0
                override fun estimateOverallAccuracy(): Double = 0.0
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

    override fun estimateOverallAccuracy(): Double {
        return delegate.estimateOverallAccuracy()
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

        val n = config.maxRepairAttemptsInResponseClassification

        repeat(n) {
            val classification = classify(call)

            val p = classification.probabilityOf400()

            val repair = when(config.aiClassifierRepairActivation){
                EMConfig.AIClassificationRepairActivation.THRESHOLD ->
                    p >= config.classificationRepairThreshold

                EMConfig.AIClassificationRepairActivation.PROBABILITY ->
                    randomness.nextBoolean(p)
            }

            if(repair){
                repairAction(call, classification)
            } else {
                return
            }
        }
        //TODO
    }



    private fun repairAction(
        call: RestCallAction,
        classification: AIResponseClassification
    ) {
        call.randomize(randomness, true)

        /*
            TODO: in the future we might want to only modify the variables that break the constraints.
            This information might be available when using a Decision Tree, but likely not for a Neural Network.
            Anyway, AIResponseClassification would need to be extended to handle this extra info, when available.
         */
    }

    /**
     * Only needed during testing, to avoid modifying model while evaluating manually crafted actions
     */
    fun disableLearning(){
        enabledLearning = false
    }
}
