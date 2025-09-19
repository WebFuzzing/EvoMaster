package org.evomaster.core.problem.rest.aiclassification

import bar.examples.it.spring.aiclassification.basic.BasicController
import bar.examples.it.spring.aiclassification.multitype.MultiTypeController
import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.kde.KDE400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.kde.KDE400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.nn.NN400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.nn.NN400EndpointModel
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType


class AIModelsCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
             initClass(BasicController())
//            initClass(MultiTypeController())
//             initClass(AllOrNoneController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheck()
            init()
            test.initializeTest()

            test.injector.injectMembers(test)

            test.setup(test.modelName)

            test.runClassifierExample()
        }
    }

    val modelName = "KNN" // Choose "GAUSSIAN", "KNN", "GLM", "KDE", "NN", etc.
    val runTimeDuration = 10_000L
    val encoderType4Test = EMConfig.EncoderType.NORMAL

    @Inject
    lateinit var randomness: Randomness

    val warmUpRep = when (modelName) {
        "NN" -> 10
        else -> 10
    }

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", modelName))
    }

    @Inject
    lateinit var config: EMConfig

    fun setup(modelName: String) {
        config.aiModelForResponseClassification = EMConfig.AIResponseClassifierModel.valueOf(modelName)
        config.aiResponseClassifierWarmup = warmUpRep
        config.aiEncoderType = encoderType4Test
        config.enableSchemaConstraintHandling = true
        config.allowInvalidData = false
        config.probRestDefault = 0.0
        config.probRestExamples = 0.0
    }

    fun executeRestCallAction(action: RestCallAction, baseUrlOfSut: String): RestCallResult {
        val fullUrl = "$baseUrlOfSut${action.resolvedPath()}"
        val url = URL(fullUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = action.verb.name
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val result = RestCallResult(action.getLocalId())

        try {
            val status = connection.responseCode
            result.setStatusCode(status)

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            result.setBody(body)
            result.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        } catch (e: Exception) {
            result.setTimedout(true)
            result.setBody("ERROR: ${e.message}")
        }

        return result
    }

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()

        // Use Gaussian400Classifier wrapper
        val globalClassifier = when(modelName){
            "GAUSSIAN" -> Gaussian400Classifier(encoderType = config.aiEncoderType, warmup = config.aiResponseClassifierWarmup, randomness = randomness)
            "KNN" -> KNN400Classifier(encoderType = config.aiEncoderType, warmup = config.aiResponseClassifierWarmup, k = 3, randomness = randomness)
            "GLM" -> GLM400Classifier(encoderType = config.aiEncoderType, warmup = config.aiResponseClassifierWarmup, learningRate = config.aiResponseClassifierLearningRate, randomness = randomness)
            "KDE" -> KDE400Classifier(encoderType = config.aiEncoderType, warmup = config.aiResponseClassifierWarmup, randomness = randomness)
            "NN" -> NN400Classifier(encoderType = config.aiEncoderType, warmup = config.aiResponseClassifierWarmup, learningRate = config.aiResponseClassifierLearningRate, randomness = randomness)
            else -> throw IllegalArgumentException("Unsupported model: $modelName")
        }

        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < runTimeDuration) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val endPoint = sampledAction.endpoint

            println("*************************************************")
            println("Path: $endPoint")

            val geneValues = sampledAction.parameters
                .map { it.primaryGene().getValueAsRawString().replace("EVOMASTER", "") }
            println("Input Genes: ${geneValues.joinToString(", ")}")
            println("Genes Size: ${geneValues.size}")

            val individual =
                sampler.createIndividual(SampleType.RANDOM, listOf(sampledAction).toMutableList())
            val action = individual.seeMainExecutableActions()[0]

            val encoderTemp = InputEncoderUtilWrapper(action, encoderType = config.aiEncoderType)

            val hasUnsupportedGene = !encoderTemp.areAllGenesSupported()
            if (hasUnsupportedGene) {
                println("Skipping classification for $endPoint as it has unsupported genes")
                // still execute the action if you want, but don’t encode or classify
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                continue
            }

            val inputVector = encoderTemp.encode()
            println("Encoded features: ${inputVector.joinToString(", ")}")
            println("Input vector size: ${inputVector.size}")

            // warmup detection
            val endpointModel: Any? = when(modelName){
                "GAUSSIAN" -> {
                    val m = (globalClassifier as Gaussian400Classifier).getModel(endPoint)
                    val isCold = (m?.modelAccuracyFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding GAUSSIAN model is cold: $isCold")
                    m
                }
                "GLM" -> {
                    val m = (globalClassifier as GLM400Classifier).getModel(endPoint)
                    val isCold = (m?.modelAccuracyFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding GLM model is cold: $isCold")
                    m
                }
                "KDE" -> {
                    val m = (globalClassifier as KDE400Classifier).getModel(endPoint)
                    val isCold = (m?.modelAccuracyFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding KDE model is cold: $isCold")
                    m
                }
                "NN" -> {
                    val m = (globalClassifier as NN400Classifier).getModel(endPoint)
                    val isCold = (m?.modelAccuracyFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding NN model is cold: $isCold")
                    m
                }
                "KNN" -> {
                    val m = (globalClassifier as KNN400Classifier).getModel(endPoint)
                    val isCold = (m?.modelAccuracyFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding KNN model is cold: $isCold")
                    m
                }
                else -> throw IllegalArgumentException("Unsupported model: $modelName")
            }

            val isCold = when(endpointModel){
                is Gaussian400EndpointModel -> endpointModel.modelAccuracyFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is GLM400EndpointModel -> endpointModel.modelAccuracyFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is KDE400EndpointModel -> endpointModel.modelAccuracyFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is NN400EndpointModel -> endpointModel.modelAccuracyFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is KNN400EndpointModel -> endpointModel.modelAccuracyFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                else -> true
            }

            if (isCold) {
                println("Warmup for endpoint $endPoint")
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                globalClassifier.updateModel(action, result)
                continue
            }

            when (endpointModel) {
                is Gaussian400EndpointModel -> {
                    println("The model is a Gaussian400EndpointModel")
                    println("Correct Predictions: ${endpointModel.modelAccuracyFullHistory.correctPrediction}")
                    println("Total Requests: ${endpointModel.modelAccuracyFullHistory.totalSentRequests}")
                    println("Accuracy: ${endpointModel.modelAccuracyFullHistory.estimateAccuracy()}")
                }
                is GLM400EndpointModel -> {
                    println("The model is a GLM400EndpointModel")
                    println("Correct Predictions: ${endpointModel.modelAccuracyFullHistory.correctPrediction}")
                    println("Total Requests: ${endpointModel.modelAccuracyFullHistory.totalSentRequests}")
                    println("Accuracy: ${endpointModel.modelAccuracyFullHistory.estimateAccuracy()}")
                }
                is KDE400EndpointModel -> {
                    println("The model is a KDE400EndpointModel")
                    println("Correct Predictions: ${endpointModel.modelAccuracyFullHistory.correctPrediction}")
                    println("Total Requests: ${endpointModel.modelAccuracyFullHistory.totalSentRequests}")
                    println("Accuracy: ${endpointModel.modelAccuracyFullHistory.estimateAccuracy()}")
                }
                is NN400EndpointModel -> {
                    println("The model is a NN400EndpointModel")
                    println("Correct Predictions: ${endpointModel.modelAccuracyFullHistory.correctPrediction}")
                    println("Total Requests: ${endpointModel.modelAccuracyFullHistory.totalSentRequests}")
                    println("Accuracy: ${endpointModel.modelAccuracyFullHistory.estimateAccuracy()}")
                }
                is KNN400EndpointModel -> {
                    println("The model is a KNN400EndpointModel")
                    println("Correct Predictions: ${endpointModel.modelAccuracyFullHistory.correctPrediction}")
                    println("Total Requests: ${endpointModel.modelAccuracyFullHistory.totalSentRequests}")
                    println("Accuracy: ${endpointModel.modelAccuracyFullHistory.estimateAccuracy()}")
                }
                else -> println("No performance info available")
            }

            println("Overall Accuracy Global: ${globalClassifier.estimateOverallAccuracy()}")

            val classification = globalClassifier.classify(action)
            val predictionOfStatusCode = classification.prediction()
            println("Prediction is: $predictionOfStatusCode")

            val sendOrNot =
                predictionOfStatusCode != 400 ||
                        Math.random() > globalClassifier.estimateAccuracy(endPoint)
            println("Send the request or not: $sendOrNot")

            if (sendOrNot) {
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                println("True Response: ${result.getStatusCode()}")
                println("Updating the classifier!")
                globalClassifier.updateModel(action, result)

                if (endpointModel is Gaussian400EndpointModel) {
                    val d400 = endpointModel.density400!!
                    val dNot400 = endpointModel.densityNot400!!

                    fun formatStats(name: String, mean: List<Double>, variance: List<Double>, n: Int) {
                        val m = mean.map { "%.2f".format(it) }
                        val v = variance.map { "%.2f".format(it) }
                        println("$name: n=$n, mean=$m, variance=$v * I_${endpointModel.dimension}")
                    }

                    formatStats("DensityNot400", dNot400.mean, dNot400.variance, dNot400.n)
                    formatStats("Density400", d400.mean, d400.variance, d400.n)

                } else if (endpointModel is KNN400EndpointModel) {
                    println("KNN stats: stored ${endpointModel.samples.size} samples")
                    // you could also print neighbors, votes, etc if useful
                }else if (endpointModel is GLM400EndpointModel) {
                    println("Updating the $modelName classifier!")
                    println("Weights and Bias = ${endpointModel.getModelParams()}")
                }
            }
        }
    }
}
