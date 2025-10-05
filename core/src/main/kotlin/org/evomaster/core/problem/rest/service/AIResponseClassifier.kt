package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.ModelEvaluation
import org.evomaster.core.problem.rest.classifier.deterministic.Deterministic400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.kde.KDE400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.nn.NN400Classifier
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
                Gaussian400Classifier(
                    warmup = config.aiResponseClassifierWarmup,
                    encoderType=config.aiEncoderType,
                    metricType =config.aIClassificationMetrics,
                    randomness = randomness)
            EMConfig.AIResponseClassifierModel.GLM ->
                GLM400Classifier(
                    warmup = config.aiResponseClassifierWarmup,
                    encoderType=config.aiEncoderType,
                    metricType =config.aIClassificationMetrics,
                    randomness = randomness,
                    learningRate = config.aiResponseClassifierLearningRate)
            EMConfig.AIResponseClassifierModel.NN ->
                NN400Classifier(
                    warmup = config.aiResponseClassifierWarmup,
                    encoderType=config.aiEncoderType,
                    metricType =config.aIClassificationMetrics,
                    randomness = randomness,
                    learningRate = config.aiResponseClassifierLearningRate)
            EMConfig.AIResponseClassifierModel.KNN ->
                KNN400Classifier(
                    warmup = config.aiResponseClassifierWarmup,
                    encoderType=config.aiEncoderType,
                    metricType =config.aIClassificationMetrics,
                    randomness = randomness,
                    k = 3)
            EMConfig.AIResponseClassifierModel.KDE ->
                KDE400Classifier(
                    warmup = config.aiResponseClassifierWarmup,
                    encoderType=config.aiEncoderType,
                    metricType =config.aIClassificationMetrics,
                    randomness = randomness
                )
            EMConfig.AIResponseClassifierModel.DETERMINISTIC ->
                Deterministic400Classifier(config.classificationRepairThreshold)
            else -> object : AIModel {
                override fun updateModel(input: RestCallAction, output: RestCallResult) {}
                override fun classify(input: RestCallAction) = AIResponseClassification()
                override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation  = ModelEvaluation.DEFAULT_NO_DATA
                override fun estimateOverallMetrics(): ModelEvaluation  = ModelEvaluation.DEFAULT_NO_DATA
            }
        }
    }


    override fun updateModel(input: RestCallAction, output: RestCallResult) {
        // Skip empty action
        if (input.parameters.isEmpty()) {
            return
        }
        if(enabledLearning) {
            delegate.updateModel(input, output)
        } else {
            log.warn("Trying to update model, but learning is disabled. This should ONLY happen when running tests in EM")
        }
    }


    override fun classify(input: RestCallAction): AIResponseClassification {
        // treat empty action as "unknown", avoid touching the model
        if (input.parameters.isEmpty()) {
            return AIResponseClassification()
        }
        return delegate.classify(input)
    }

    override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation {
        return delegate.estimateMetrics(endpoint)
    }


    override fun estimateOverallMetrics(): ModelEvaluation {
        return delegate.estimateOverallMetrics()
    }


    fun viewInnerModel(): AIModel = delegate

    /**
     * If the model thinks this call will lead to a user error (e.g., 400), then try to repair
     * the action to be able to solve the input constraints, aiming for a 2xx.
     * There is no guarantee that this will work.
     */
    fun attemptRepair(call: RestCallAction){

        val metrics = estimateMetrics(call.endpoint)

        /**
         * Skips repair if the classifier is still weak, as indicated by low accuracy and F1-score
         * (see [ModelEvaluation]). A threshold of accuracy > 0.5 ensures that the classifier performs
         * better than random guessing overall, while an F1-score > 0.2 ensures at least minimal skill
         * in identifying 400 responses. If either threshold is not met, the classifier is not yet
         * reliable for guiding repairs. In such cases, the call is executed as originally generated
         * to allow the classifier to gather more informative data and improve over time.
         */
        if(!(metrics.accuracy > 0.5 && metrics.f1Score400 > 0.2)){
            //do nothing
            return
        }

        val n = config.maxRepairAttemptsInResponseClassification

        repeat(n) {
            val classification = classify(call)
            val p = classification.probabilityOf400()

            // Stop attempts to repair if the classifier predicts a non-400 response
            if (classification.prediction()!=400) return

            val repair = when(config.aiClassifierRepairActivation){

                /**
                 * Threshold-based decision-making:
                 * Attempts repair only if the predicted probability of failure (p)
                 * is greater than or equal to the configured threshold.
                 */
                EMConfig.AIClassificationRepairActivation.THRESHOLD ->
                    p >= config.classificationRepairThreshold

                /**
                 * Probabilistic decision-making:
                 * Attempts repair with probability equal to the predicted probability of failure (p).
                 * If p is high, repair is more likely; if p is low, repair is less likely.
                 * This approach reduces unnecessary repairs while still allowing exploration
                 * of potential misclassifications.
                 */
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
     * Only needed during testing to avoid modifying the model while evaluating manually crafted actions
     */
    fun disableLearning(){
        enabledLearning = false
    }

}
