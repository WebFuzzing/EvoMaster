package org.evomaster.core.problem.rest.aiclassification

import bar.examples.it.spring.aiclassification.allornone.AllOrNoneController
import com.google.inject.Inject
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.probabilistic.InputEncoderUtilWrapper
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.gaussian.Gaussian400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.glm.GLM400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.kde.KDE400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400Classifier
import org.evomaster.core.problem.rest.classifier.probabilistic.knn.KNN400EndpointModel
import org.evomaster.core.problem.rest.classifier.probabilistic.nn.NN400Classifier
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness


class AIModelsCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
//            initClass(MultiTypeController())


             initClass(AllOrNoneController())
//            initClass(ArithmeticController())
//             initClass(BasicController())
//            initClass(ImplyController())
//             initClass(MixedController())
//             initClass(OnlyOneController())
//             initClass(OrController())
//             initClass(RequiredController())
//             initClass(ZeroOrOneController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheck()
            init()                        // initialize controllers
            test.initializeTest()         // create injector

            test.injector.injectMembers(test)  // injects config, classifier, etc.

            test.setup(test.modelName)         // setup based on the config

            test.runClassifierExample()
        }
    }

    val modelName = "KNN" // Choose "GAUSSIAN", "GLM", "KDE", "KNN", "NN", etc.
    val runIterations = 5000
    val encoderType4Test = EMConfig.EncoderType.RAW
    val decisionMaking = "PROBABILITY"
    val saveReport = false

    @Inject
    lateinit var randomness: Randomness

    val warmUpRep = when (modelName) {
        "NN" -> 1000
        else -> 10
    }

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", "$modelName"))
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

    @Inject
    lateinit var aiGlobalClassifier: AIResponseClassifier


    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()

        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)

        for (i in 0 until runIterations) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val endPoint = sampledAction.endpoint

            println("*************************************************")
            println("Iteration $i | Path: $endPoint")

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
                        .joinToString(", ") { ng ->
                            "${ng.gene.name}:${ng.gene::class.simpleName ?: "Unknown"}" })

            val hasUnsupportedGene = !encoderTemp.areAllGenesSupported()
            if (hasUnsupportedGene) {
                println("Skipping classification for $endPoint as it has unsupported genes")
                continue
            }

            val inputVector = encoderTemp.encode()
            println("Encoded features: ${inputVector.joinToString(", ")}")
            println("Input vector size: ${inputVector.size}")

            // Warm-up
            val innerModel = aiGlobalClassifier.viewInnerModel()
            println("innerModel is ${innerModel.javaClass.simpleName ?: "Unknown"}")
            val endpointModel = when(innerModel) {
                is Gaussian400Classifier -> innerModel.getModel(endPoint)
                is GLM400Classifier      -> innerModel.getModel(endPoint)
                is KDE400Classifier      -> innerModel.getModel(endPoint)
                is KNN400Classifier      -> innerModel.getModel(endPoint)
                is NN400Classifier       -> innerModel.getModel(endPoint)
                else -> throw IllegalArgumentException("Unsupported model: $modelName")
            }

            val isCold = (endpointModel?.modelMetricsFullHistory?.totalSentRequests ?: 0) < config.aiResponseClassifierWarmup

            if (isCold) {
                println("Warmup for endpoint $endPoint")
                val result = ExtraTools.executeRestCallAction(action, "$baseUrlOfSut")
                aiGlobalClassifier.updateModel(action, result)
                continue
            }

            endpointModel?.let {
                ExtraTools.printModelMetrics("${it.javaClass.simpleName}", it.modelMetricsFullHistory)
            } ?: println("No endpoint model available yet for $endPoint")

            val overAllMetrics = aiGlobalClassifier.estimateOverallMetrics()
            println("Overall Accuracy: ${overAllMetrics.accuracy}")
            println("Overall Precision400: ${overAllMetrics.precision400}")
            println("Overall Recall400: ${overAllMetrics.recall400}")
            println("Overall F1Score400: ${overAllMetrics.f1Score400}")
            println("Overall MCC: ${overAllMetrics.mcc}")

            val classification = aiGlobalClassifier.classify(action)
            val predictionOfStatusCode = classification.prediction()
            println("Prediction is: $predictionOfStatusCode")

            val sendOrNot: Boolean
            when (decisionMaking) {
                "THRESHOLD" -> {
                    sendOrNot = true
                }
                // Probabilistic decision-making based on mcc
                "PROBABILITY" -> {
                    val mcc = aiGlobalClassifier.estimateMetrics(endPoint).mcc
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

            println("Send the request or not: $sendOrNot")

            if (sendOrNot) {
                val result = ExtraTools.executeRestCallAction(action, "$baseUrlOfSut")
                println("True Response: ${result.getStatusCode()}")
                println("Updating the classifier!")
                aiGlobalClassifier.updateModel(action, result)

                when (endpointModel) {
                    is Gaussian400EndpointModel -> {
                        val d400 = endpointModel.density400!!
                        val dNot400 = endpointModel.densityNot400!!

                        fun formatStats(name: String, mean: List<Double>, variance: List<Double>, n: Int) {
                            val m = mean.map { "%.2f".format(it) }
                            val v = variance.map { "%.2f".format(it) }
                            println("$name: n=$n, mean=$m, variance=$v * I_${endpointModel.dimension}")
                        }

                        formatStats("DensityNot400", dNot400.mean, dNot400.variance, dNot400.n)
                        formatStats("Density400", d400.mean, d400.variance, d400.n)

                    }

                    is KNN400EndpointModel -> {
                        println("KNN stats: stored ${endpointModel.samples.size} samples")
                        // you could also print neighbors, votes, etc if useful
                    }

                    is GLM400EndpointModel -> {
                        println("Updating the $modelName classifier!")
                        println("Weights and Bias = ${endpointModel.getModelParams()}")
                    }
                }
            }
        }

        // Save the final result as a .txt file
        if (saveReport){
            val innerModel = aiGlobalClassifier.viewInnerModel()
            val allModels = when (innerModel) {
                is Gaussian400Classifier -> innerModel.getAllModels()
                is GLM400Classifier      -> innerModel.getAllModels()
                is KDE400Classifier      -> innerModel.getAllModels()
                is KNN400Classifier      -> innerModel.getAllModels()
                is NN400Classifier       -> innerModel.getAllModels()
                else -> throw IllegalArgumentException("Unsupported model: $modelName")
            }
            ExtraTools.saveAllMetricsToTxt(
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
