package org.evomaster.core.problem.rest.aiclassification

import bar.examples.it.spring.aiclassification.allornone.AllOrNoneController
import com.google.inject.Inject
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
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
import org.evomaster.core.problem.rest.classifier.quantifier.ModelMetricsFullHistory
import org.evomaster.core.problem.rest.classifier.quantifier.ModelMetricsWithTimeWindow
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness

class AIModelsCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(AllOrNoneController())
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

    val modelName = "KNN" // Choose "GAUSSIAN", "GLM", "KDE", "KNN", "NN", etc.
    val encoderType = "RAW" // Choose "RAW" or "NORMAL"
    val decisionMaking = "PROBABILITY" // Choose "PROBABILITY" or "THRESHOLD"
    val metricType = "TIME_WINDOW"
    val warmUpRep = 10
    val maxAttemptRepair = 100 // i.e., the classifier has 10 times the chances to pick an action with non-400 response

    val runIterations = 500
    val saveReport = false
    val filePathReport = "AIModelsCheckReport.txt"

    @Inject
    lateinit var randomness: Randomness

    @Inject
    lateinit var config: EMConfig

    @Inject
    lateinit var aiGlobalClassifier: AIResponseClassifier

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", modelName))
    }

    fun setup(modelName: String) {
        config.setAIModels(EMConfig.AIResponseClassifierModel.valueOf(modelName))
        config.aiEncoderType = EMConfig.EncoderType.valueOf(encoderType)
        config.aiClassifierRepairActivation =
            EMConfig.AIClassificationRepairActivation.valueOf(decisionMaking)
        config.aIClassificationMetrics =
            EMConfig.AIClassificationMetrics.valueOf(metricType)
        config.aiResponseClassifierWarmup = warmUpRep
        config.maxRepairAttemptsInResponseClassification = maxAttemptRepair
    }

    fun repairAction(call: RestCallAction) {
        call.randomize(randomness, true)
    }

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()

        RestActionBuilderV3.addActionsFromSwagger(
            restSchema,
            actionCluster,
            options = options
        )

        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()

        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)

        for (i in 0 until runIterations) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val endPoint = sampledAction.endpoint

            println("*************************************************")
            println("Iteration $i")
            println("Path: $endPoint")

            val geneValues = sampledAction.parameters
                .map { it.primaryGene().getValueAsRawString().replace("EVOMASTER", "") }

            println("Input Genes: ${geneValues.joinToString(", ")}")
            println("Genes Size: ${geneValues.size}")

            val individual =
                sampler.createIndividual(
                    SampleType.RANDOM,
                    listOf(sampledAction).toMutableList()
                )

            val action = individual.seeMainExecutableActions()[0]

            val encoder = InputEncoderUtilWrapper(
                action,
                encoderType = config.aiEncoderType
            )

            println(
                "Expanded genes are: " +
                        encoder.endPointToGeneList()
                            .joinToString(", ") { ng ->
                                "${ng.gene.name}:${ng.gene::class.simpleName ?: "Unknown"}"
                            }
            )

            if (encoder.areAllGenesUnSupported()) {
                println("Skipping classification for $endPoint as all its genes are unsupported.")
                continue
            }

            val inputVector = encoder.encode()
            println("Encoded features: ${inputVector.joinToString(", ")}")
            println("Input vector size: ${inputVector.size}")

            printEndpointModels(endPoint)

            val metrics = aiGlobalClassifier.estimateMetrics(action.endpoint)

            if (!(metrics.accuracy > 0.5 && metrics.f1Score400 > 0.5)) {
                println("The classifier is weak for $endPoint")

                val result = ExtraTools.executeRestCallAction(action, baseUrlOfSut)
                println("True Response: ${result.getStatusCode()}")

                println("Updating the classifier!")
                aiGlobalClassifier.updateModel(action, result)

            } else {
                println("The classifier is good enough for $endPoint")

                val n = config.maxRepairAttemptsInResponseClassification

                for (j in 0 until n) {
                    val classification = aiGlobalClassifier.classify(action)
                    val p = classification.probabilityOf400()

                    val predictionOfStatusCode = classification.prediction()

                    if (predictionOfStatusCode == 400) {
                        val repairOrNot = when (config.aiClassifierRepairActivation) {
                            EMConfig.AIClassificationRepairActivation.THRESHOLD ->
                                p >= config.classificationRepairThreshold

                            EMConfig.AIClassificationRepairActivation.PROBABILITY ->
                                randomness.nextBoolean(p)
                        }

                        if (repairOrNot) {
                            repairAction(action)
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                val result = ExtraTools.executeRestCallAction(action, baseUrlOfSut)
                println("True Response: ${result.getStatusCode()}")

                println("Updating the classifier!")
                aiGlobalClassifier.updateModel(action, result)
            }

            printEndpointModels(endPoint)
        }

        val overAllMetrics = aiGlobalClassifier.estimateOverallMetrics()

        println("Overall Accuracy: ${overAllMetrics.accuracy}")
        println("Overall Precision400: ${overAllMetrics.precision400}")
        println("Overall Recall400: ${overAllMetrics.sensitivity400}")
        println("Overall F1Score400: ${overAllMetrics.f1Score400}")
        println("Overall MCC: ${overAllMetrics.mcc}")

        if (saveReport) {
            saveReports()
        }

        println("The process is finished!")
    }

    private fun printEndpointModels(endPoint: org.evomaster.core.problem.rest.data.Endpoint) {
        val innerModels = aiGlobalClassifier.viewInnerModels()

        println("innerModels is ${innerModels.javaClass.simpleName}")

        for (innerModel in innerModels) {
            println("innerModel is ${innerModel.javaClass.simpleName}")

            val endpointModel = when (innerModel) {
                is Gaussian400Classifier -> innerModel.getModel(endPoint)
                is GLM400Classifier -> innerModel.getModel(endPoint)
                is KDE400Classifier -> innerModel.getModel(endPoint)
                is KNN400Classifier -> innerModel.getModel(endPoint)
                is NN400Classifier -> innerModel.getModel(endPoint)

                else -> throw IllegalArgumentException(
                    "Unsupported model type: ${innerModel::class.simpleName}"
                )
            }

            endpointModel?.let {
                val metrics = it.modelMetrics

                when (metrics) {
                    is ModelMetricsWithTimeWindow ->
                        ExtraTools.printModelMetrics("${it.javaClass.simpleName}", metrics)

                    is ModelMetricsFullHistory ->
                        ExtraTools.printModelMetrics("${it.javaClass.simpleName}", metrics)

                    else -> throw IllegalArgumentException(
                        "Unsupported metrics type: ${metrics::class.simpleName}"
                    )
                }
            }

            when (endpointModel) {
                is Gaussian400EndpointModel -> {
                    val d400 = endpointModel.density400!!
                    val dNot400 = endpointModel.densityNot400!!

                    fun formatStats(
                        name: String,
                        mean: List<Double>,
                        variance: List<Double>,
                        n: Int
                    ) {
                        val m = mean.map { "%.2f".format(it) }
                        val v = variance.map { "%.2f".format(it) }

                        println(
                            "$name: n=$n, mean=$m, variance=$v * I_${endpointModel.dimension}"
                        )
                    }

                    formatStats(
                        "DensityNot400",
                        dNot400.mean,
                        dNot400.variance,
                        dNot400.n
                    )

                    formatStats(
                        "Density400",
                        d400.mean,
                        d400.variance,
                        d400.n
                    )
                }

                is GLM400EndpointModel -> {
                    println("Weights and Bias = ${endpointModel.getModelParams()}")
                }

                is KNN400EndpointModel -> {
                    println("KNN stats: stored ${endpointModel.samples.size} samples")
                }

                is NN400EndpointModel,
                is KDE400EndpointModel -> {
                    println("The model is ${innerModel.javaClass.simpleName}.")
                }

                null -> {
                    println("No endpoint model exists yet for $endPoint.")
                }
            }
        }
    }

    private fun saveReports() {
        val innerModels = aiGlobalClassifier.viewInnerModels()

        for (innerModel in innerModels) {
            val allModels = when (innerModel) {
                is Gaussian400Classifier -> innerModel.getAllModels()
                is GLM400Classifier -> innerModel.getAllModels()
                is KDE400Classifier -> innerModel.getAllModels()
                is KNN400Classifier -> innerModel.getAllModels()
                is NN400Classifier -> innerModel.getAllModels()

                else -> throw IllegalArgumentException(
                    "Unsupported model type: ${innerModel::class.simpleName}"
                )
            }

            val modelLabel = innerModel.javaClass.simpleName ?: modelName

            val reportPath =
                if (innerModels.size == 1) {
                    filePathReport
                } else {
                    filePathReport.removeSuffix(".txt") + "_$modelLabel.txt"
                }

            ExtraTools.saveAllMetricsToTxt(
                allModels,
                reportPath,
                runIterations,
                config.aiEncoderType,
                modelLabel
            )

            println("The report is saved for $modelLabel!")
        }
    }
}