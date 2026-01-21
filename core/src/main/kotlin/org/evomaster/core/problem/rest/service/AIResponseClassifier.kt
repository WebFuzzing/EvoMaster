package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.classifier.AIModel
import org.evomaster.core.problem.rest.classifier.AIResponseClassification
import org.evomaster.core.problem.rest.classifier.quantifier.ModelEvaluation
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

    // For statistics
    private var updateTimeNs: Long = 0
    private var updateCount: Long = 0
    private var classifyTimeNs: Long = 0
    private var classifyCount: Long = 0
    private var repairTimeNs: Long = 0
    private var repairCount: Long = 0
    private var observed200Count: Long = 0
    private var observed400Count: Long = 0
    private var observed500Count: Long = 0

    fun getUpdateTimeNs(): Long = updateTimeNs
    fun getUpdateCount(): Long = updateCount
    fun getClassifyTimeNs(): Long = classifyTimeNs
    fun getClassifyCount(): Long = classifyCount
    fun getRepairTimeNs(): Long = repairTimeNs
    fun getRepairCount(): Long = repairCount
    fun getObserved200Count(): Long = observed200Count
    fun getObserved400Count(): Long = observed400Count
    fun getObserved500Count(): Long = observed500Count

    
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
                Deterministic400Classifier(
                    config.classificationRepairThreshold,
                    metricType = config.aIClassificationMetrics)
            else -> object : AIModel {
                override fun updateModel(input: RestCallAction, output: RestCallResult) {}
                override fun classify(input: RestCallAction) = AIResponseClassification()
                override fun estimateMetrics(endpoint: Endpoint): ModelEvaluation  = ModelEvaluation.DEFAULT_NO_DATA
                override fun estimateOverallMetrics(): ModelEvaluation  = ModelEvaluation.DEFAULT_NO_DATA
            }
        }
    }

    override fun updateModel(input: RestCallAction, output: RestCallResult) {

        if (skipUpdate(input, output)) {
            return
        }

        if(enabledLearning) {

            val start = System.nanoTime()
            delegate.updateModel(input, output)
            val t = System.nanoTime() - start

            updateTimeNs += t
            updateCount++

        } else {
            log.warn("Trying to update model, but learning is disabled. This should ONLY happen when running tests in EM")
        }
    }


    override fun classify(input: RestCallAction): AIResponseClassification {
        // treat empty action as "unknown", avoid touching the model
        if (input.parameters.isEmpty()) {
            return AIResponseClassification()
        }

        val start = System.nanoTime()
        val result = delegate.classify(input)
        val t = System.nanoTime() - start

        classifyTimeNs += t
        classifyCount++

        return result
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
         * Skips repair when the classifier is still too weak to provide meaningful guidance.
         * Reliability is assessed using precision, recall, and MCC (see [ModelEvaluation]) to
         * ensure the model performs better than random guessing, especially important under the class imbalance.
         * If any of the criteria are not met, the classifier is considered unreliable
         * for steering repairs. In such cases, the call is executed without modification so the
         * classifier can gather additional informative samples and improve over time.
         */
        val weaknessThreshold = config.aIResponseClassifierWeaknessThreshold
        if (metrics.mcc <= weaknessThreshold
            || metrics.precision400 <= weaknessThreshold
            || metrics.recall400 <= weaknessThreshold) {

            //do nothing
            return
        }

        val n = config.maxRepairAttemptsInResponseClassification

        repeat(n) {
            val classification = classify(call)
            val p = classification.probabilityOf400()

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
                 * Attempts repair with a probability equal to the predicted failure probability (p), provided that
                 * p exceeds the classification threshold which is 0.5 from [AIResponseClassification.prediction].
                 * If p is high, repair is more likely; if p is low, repair is less likely.
                 * This approach reduces unnecessary repairs while still allowing exploration
                 * of potential misclassifications.
                 */
                EMConfig.AIClassificationRepairActivation.PROBABILITY ->
                    randomness.nextBoolean(p) && p > 0.5
            }

            if(repair){
                val start = System.nanoTime()
                repairAction(call, classification)
                val t = System.nanoTime() - start

                repairTimeNs += t
                repairCount++

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
     * Decide whether based on the observation, the model update should be skipped or not.
     */
    private fun skipUpdate(input: RestCallAction, output: RestCallResult): Boolean {

        // skip if no input parameters
        if (input.parameters.isEmpty()) return true

        // Get the status code and skip if it is null
        val trueStatusCode = output.getStatusCode() ?: return true

        // update counters
        when (trueStatusCode) {
            200 -> observed200Count++
            400 -> observed400Count++
            500 -> observed500Count++
        }
        // skip conditions
        val skip500 =
            trueStatusCode == 500 && config.skipAIModelUpdateWhenResponseIs500

        val skipNot200Or400 =
            trueStatusCode != 200
                && trueStatusCode != 400
                    && config.skipAIModelUpdateWhenResponseIsNot200Or400

        return skip500 || skipNot200Or400

    }


    /**
     * Only needed during testing to avoid modifying the model while evaluating manually crafted actions
     */
    fun disableLearning(){
        enabledLearning = false
    }

}
