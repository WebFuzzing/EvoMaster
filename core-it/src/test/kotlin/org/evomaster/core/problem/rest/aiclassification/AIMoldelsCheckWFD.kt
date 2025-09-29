package org.evomaster.core.problem.rest.aiclassification

import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.ModelMetricsFullHistory
import org.evomaster.core.problem.rest.classifier.probabilistic.AbstractProbabilistic400EndpointModel
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
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType
import java.io.File


class AIModelsCheckWFD : IntegrationTestRestBase() {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheckWFD()
            test.initializeTest()

            test.injector.injectMembers(test)

            test.setup(test.modelName)

            test.runClassifierExample()
        }
    }

    val modelName = "KNN" // Choose "GAUSSIAN", "KNN", "GLM", "KDE", "NN", etc.
    val runIterations = 5000
    val encoderType4Test = EMConfig.EncoderType.RAW
    val saveReport: Boolean = false

    val decisionMaking = "sendAnyway" // choose "probabilistic" to make the machine decide weather to send the request or not

    val baseUrlOfSut = "http://localhost:8080"
    val v2orV3 = "v2"

    val swaggerUrl = "$baseUrlOfSut/$v2orV3/api-docs"


    @Inject
    lateinit var randomness: Randomness

    val warmUpRep = when (modelName) {
        "NN" -> 10
        else -> 10
    }


    fun recreateInjectorForBlackBox(extraArgs: List<String> = listOf()) {
        val args = listOf(
            "--problemType", "REST",
            "--blackBox", "true",
            "--bbTargetUrl", baseUrlOfSut,
            "--bbSwaggerUrl", swaggerUrl,
            "--createConfigPathIfMissing", "false",
            "--seed", "42"
        ).plus(extraArgs)

        injector = init(args)
    }

    fun initializeTest() {
        recreateInjectorForBlackBox(listOf(modelName))
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

    private fun printModelMetrics(modelName: String, metrics: ModelMetricsFullHistory) {
        println("=== $modelName ===")
        println(
            """
        +-------------------------------------------+
        +----- Confusion Matrix 400 vs. not400 -----+
        +-------------------------------------------+
        |            | Pred 400     | Pred ¬400     |
        +-------------------------------------------+
        | Actual 400 | TP=${metrics.truePositive400.toString().padEnd(10)}| FN=${metrics.falseNegative400.toString().padEnd(11)}|
        | Actual¬400 | FP=${metrics.falsePositive400.toString().padEnd(10)}| TN=${metrics.trueNegative400.toString().padEnd(11)}|
        +-------------------------------------------+
        Correct Predictions : ${metrics.correctPrediction}
        Total Requests      : ${metrics.totalSentRequests}
        Accuracy            : ${"%.4f".format(metrics.estimateMetrics().accuracy)}
        Precision400        : ${"%.4f".format(metrics.estimateMetrics().precision400)}
        Recall400           : ${"%.4f".format(metrics.estimateMetrics().recall400)}
        F1Score400          : ${"%.4f".format(metrics.estimateMetrics().f1Score400)}
        MCC400              : ${"%.4f".format(metrics.estimateMetrics().mcc)}
        """.trimIndent()
        )
    }

    fun saveAllMetricsToTxt(
        models: Map<Endpoint, AbstractProbabilistic400EndpointModel?>,
        filePath: String,
        runIterations: Int,
        encoderType: EMConfig.EncoderType,
        modelName: String
    ) {
        val sb = StringBuilder()
        sb.appendLine("=============================================")
        sb.appendLine("EvoMaster Classifier Report")
        sb.appendLine("=============================================")
        sb.appendLine("Model Type     : $modelName")
        sb.appendLine("Encoder Type   : $encoderType")
        sb.appendLine("Number of Iterations : $runIterations")
        sb.appendLine("=============================================")
        sb.appendLine()

        for ((endpoint, model) in models) {
            val metrics: ModelMetricsFullHistory? = when (model) {
                is Gaussian400EndpointModel -> model.modelMetricsFullHistory
                is GLM400EndpointModel      -> model.modelMetricsFullHistory
                is KDE400EndpointModel      -> model.modelMetricsFullHistory
                is NN400EndpointModel       -> model.modelMetricsFullHistory
                is KNN400EndpointModel      -> model.modelMetricsFullHistory
                else -> null
            }

            metrics?.let {
                sb.appendLine("Endpoint: $endpoint")
                sb.appendLine()
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("+----- Confusion Matrix 400 vs. not400 -----+")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("|            | Pred 400     | Pred ¬400     |")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine("| Actual 400 | TP=${it.truePositive400.toString().padEnd(10)}| FN=${it.falseNegative400.toString().padEnd(11)}|")
                sb.appendLine("| Actual¬400 | FP=${it.falsePositive400.toString().padEnd(10)}| TN=${it.trueNegative400.toString().padEnd(11)}|")
                sb.appendLine("+-------------------------------------------+")
                sb.appendLine()
                sb.appendLine("Correct Predictions : ${it.correctPrediction}")
                sb.appendLine("Total Requests      : ${it.totalSentRequests}")
                sb.appendLine("Accuracy            : ${"%.4f".format(it.estimateMetrics().accuracy)}")
                sb.appendLine("Precision400        : ${"%.4f".format(it.estimateMetrics().precision400)}")
                sb.appendLine("Recall400           : ${"%.4f".format(it.estimateMetrics().recall400)}")
                sb.appendLine("F1Score400          : ${"%.4f".format(it.estimateMetrics().f1Score400)}")
                sb.appendLine("MCC400              : ${"%.4f".format(it.estimateMetrics().mcc)}")
                sb.appendLine()
                sb.appendLine("=============================================")
                sb.appendLine()
            }
        }

        // Save the report
        File(filePath).writeText(sb.toString())

    }

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v2/api-docs")
//        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
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

        for (i in 0 until runIterations) {
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

            //print gene types
            println("Expanded genes are: " +
                    encoderTemp.endPointToGeneList()
                        .joinToString(", ") { it.getLeafGene()::class.simpleName ?: "Unknown" })

            val geneList = encoderTemp.endPointToGeneList()
            val typesRow = geneList.joinToString(", ") { gene -> gene.javaClass.simpleName }
            println("Genes type in the gene list: $typesRow")

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
                    val isCold = (m?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding GAUSSIAN model is cold: $isCold")
                    m
                }
                "GLM" -> {
                    val m = (globalClassifier as GLM400Classifier).getModel(endPoint)
                    val isCold = (m?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding GLM model is cold: $isCold")
                    m
                }
                "KDE" -> {
                    val m = (globalClassifier as KDE400Classifier).getModel(endPoint)
                    val isCold = (m?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding KDE model is cold: $isCold")
                    m
                }
                "NN" -> {
                    val m = (globalClassifier as NN400Classifier).getModel(endPoint)
                    val isCold = (m?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding NN model is cold: $isCold")
                    m
                }
                "KNN" -> {
                    val m = (globalClassifier as KNN400Classifier).getModel(endPoint)
                    val isCold = (m?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup
                    println("Corresponding KNN model is cold: $isCold")
                    m
                }
                else -> throw IllegalArgumentException("Unsupported model: $modelName")
            }

            val isCold = when(endpointModel){
                is Gaussian400EndpointModel -> endpointModel.modelMetricsFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is GLM400EndpointModel -> endpointModel.modelMetricsFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is KDE400EndpointModel -> endpointModel.modelMetricsFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is NN400EndpointModel -> endpointModel.modelMetricsFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                is KNN400EndpointModel -> endpointModel.modelMetricsFullHistory.totalSentRequests < config.aiResponseClassifierWarmup
                else -> true
            }

            if (isCold) {
                println("Warmup for endpoint $endPoint")
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                globalClassifier.updateModel(action, result)
                continue
            }

            when (endpointModel) {
                is Gaussian400EndpointModel -> printModelMetrics("Gaussian400EndpointModel", endpointModel.modelMetricsFullHistory)
                is GLM400EndpointModel      -> printModelMetrics("GLM400EndpointModel", endpointModel.modelMetricsFullHistory)
                is KDE400EndpointModel      -> printModelMetrics("KDE400EndpointModel", endpointModel.modelMetricsFullHistory)
                is NN400EndpointModel       -> printModelMetrics("NN400EndpointModel", endpointModel.modelMetricsFullHistory)
                is KNN400EndpointModel      -> printModelMetrics("KNN400EndpointModel", endpointModel.modelMetricsFullHistory)
                else -> println("No performance info available")
            }

            val overAllMetrics = globalClassifier.estimateOverallMetrics()
            println("Overall Accuracy: ${overAllMetrics.accuracy}")
            println("Overall Precision400: ${overAllMetrics.precision400}")
            println("Overall Recall400: ${overAllMetrics.recall400}")
            println("Overall F1Score400: ${overAllMetrics.f1Score400}")
            println("Overall MCC: ${overAllMetrics.mcc}")

            val classification = globalClassifier.classify(action)
            val predictionOfStatusCode = classification.prediction()
            println("Prediction is: $predictionOfStatusCode")

            val sendOrNot: Boolean
            when (decisionMaking) {
                "sendAnyway" -> {
                    sendOrNot = true
                }
                // Probabilistic decision-making based on mcc
                "probabilistic" -> {
                    val mcc = globalClassifier.estimateMetrics(endPoint).mcc
                    sendOrNot = if (predictionOfStatusCode != 400 || mcc <= 0.5) {
                        true
                    } else {
                        Math.random() > mcc
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported decision making strategy: $decisionMaking")
                }
            }

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

        // Save the final result as a .txt file
        if (saveReport == true){
            val allModels = globalClassifier.getAllModels()
            saveAllMetricsToTxt(
                allModels,
                "classifier_report.txt",
                runIterations,
                config.aiEncoderType,
                modelName)
            println("The report is saved!")
        }
        println("The process is finished!")

    }
}
