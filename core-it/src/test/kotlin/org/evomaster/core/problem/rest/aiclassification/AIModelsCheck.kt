package org.evomaster.core.problem.rest.aiclassification

import bar.examples.it.spring.aiclassification.allornone.AllOrNoneController
import bar.examples.it.spring.aiclassification.basic.BasicController
import bar.examples.it.spring.aiclassification.multitype.MultiTypeController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.AbstractAIModel
import org.evomaster.core.problem.rest.classifier.EncoderType
import org.evomaster.core.problem.rest.classifier.GLMModel
import org.evomaster.core.problem.rest.classifier.GaussianModel
import org.evomaster.core.problem.rest.classifier.InputEncoderUtils
import org.evomaster.core.problem.rest.classifier.KDEModel
import org.evomaster.core.problem.rest.classifier.KNNModel
import org.evomaster.core.problem.rest.classifier.NNModel
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType
import kotlin.collections.iterator


class AIModelsCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(BasicController())
//            initClass(MultiTypeController())
//            initClass(AllOrNoneController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIModelsCheck()
            init()
            test.initializeTest()
            test.runClassifierExample()
        }
    }

    // define which model you want to use
    val modelName = "KNN" // change to "GAUSSIAN", "GLM", "KNN", "KDE", "GM", "NN", etc.
    val warmupRep = when(modelName){
        "NN" -> 1000  //NN needs significant numbers of training samples to warm-up
        else -> 10
    }
    val encoderType4Test = when(modelName){
        "KNN" -> EncoderType.RAW
        "GLM" -> EncoderType.RAW
        else -> EncoderType.NORMAL
    }

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", modelName))
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
            result.setBodyType(MediaType.APPLICATION_JSON_TYPE) // or guess based on the Content-Type header

        } catch (e: Exception) {
            result.setTimedout(true)
            result.setBody("ERROR: ${e.message}")
        }

        return result
    }

    // Factory for models
    private fun createModel(modelName: String, dimension: Int, warmup: Int): AbstractAIModel =
        when (modelName.uppercase()) {
            "GAUSSIAN" -> GaussianModel().apply {
                setup(dimension, warmup)
                encoderType = encoderType4Test
            }
            "GLM" -> GLMModel().apply {
                setup(dimension, warmup)
                encoderType = encoderType4Test
            }
            "KDE" -> KDEModel().apply {
                setup(dimension, warmup)
                encoderType = encoderType4Test
            }
            "KNN" -> KNNModel(k = 3).apply {
                setup(dimension, warmup)
                encoderType = encoderType4Test
            }
            "NN" -> NNModel().apply {
                setup(dimension, warmup)
                encoderType = encoderType4Test
            }
            else -> throw IllegalArgumentException("Unknown model type: $modelName")
        }


    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val config = EMConfig().apply {
            aiModelForResponseClassification = EMConfig.AIResponseClassifierModel.valueOf(modelName)
            enableSchemaConstraintHandling = true
            allowInvalidData = false
            probRestDefault = 0.0
            probRestExamples = 0.0
        }

        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)

        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()

        val endpointToDimension = mutableMapOf<String, Int?>()

        for (action in actionList) {
            val name = action.getName()
            val encoder = InputEncoderUtils(action, encoderType = encoderType4Test)
            val listGenes = encoder.endPointToGeneList().map { gene -> gene.getLeafGene() }
            listGenes.forEach {println("Gene: ${it::class.simpleName}") }

            val hasUnsupportedGene = !encoder.areAllGenesSupported()
            println("Has unsupported gene: $hasUnsupportedGene")

            val dimension = if (hasUnsupportedGene) null else listGenes.size
            println("Endpoint: $name, dimension: $dimension")

            endpointToDimension[name] = dimension
        }

        // Initialize classifiers
        val endpointToClassifier = mutableMapOf<String, AbstractAIModel?>()
        for ((name, dimension) in endpointToDimension) {
            endpointToClassifier[name] =
                if (dimension == null) null else createModel(modelName, dimension, warmupRep)
        }

        for ((name, expectedDimension) in endpointToDimension) {
            println("The model is $modelName for $name with expected dimension as $expectedDimension")
        }

        // Execute the procedure for a period of time
        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)
        val startTime = System.currentTimeMillis()
        val runDuration = 1_000L // Milliseconds
        while (System.currentTimeMillis() - startTime < runDuration) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val name = sampledAction.getName()
            val classifier = endpointToClassifier[name]
            val dimension = endpointToDimension[name]
            val geneValues = sampledAction.parameters.map { it.primaryGene().getValueAsRawString()}

            println("*************************************************")
            println("Path         : $name")
            println("Classifier   : ${classifier?.javaClass?.simpleName ?: "null"}")
            println("Dimension    : $dimension")
            println("Input Genes  : ${geneValues.joinToString(", ")}")
            println("Genes Size   : ${geneValues.size}")

            //  executeRestCallAction is replaced with createIndividual to avoid override error
            //  val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val individual = sampler.createIndividual(SampleType.RANDOM, listOf(sampledAction).toMutableList())
            val action = individual.seeMainExecutableActions()[0]

            val encoderTemp = InputEncoderUtils(action, encoderType = encoderType4Test)
            val inputVector = encoderTemp.encode()
            println("Encoded features are: ${inputVector.joinToString(", ")}")

            println("Input vector size: ${inputVector.size}")

            // Execution without classification for the endpoints with unsupported genes
            if (classifier==null){
                executeRestCallAction(action,"$baseUrlOfSut")
                println("No classification as the classifier is null, i.e., the endpoint contains unsupported genes")
                continue
            }

            // Warmup cold classifiers by at least n request
            val isCold = classifier.performance.totalSentRequests < classifier.warmup

            println("Corresponding model for the endpoint is cold: $isCold")

            if (isCold) {
                println("Warmup the endpoint corresponding $modelName model!")
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                classifier.updateModel(action, result)
                continue
            }

            println("Correct Predictions: ${classifier.performance.correctPrediction}")
            println("Total Requests: ${classifier.performance.totalSentRequests}")
            println("Accuracy: ${classifier.performance.estimateAccuracy()}")


            // Classification
            println("Classifying by $modelName model!")
            val classification = classifier.classify(action)

            // Prediction
            val predictionOfStatusCode = classification.prediction()
            println("Prediction is : $predictionOfStatusCode")

            // Probabilistic decision-making based on Bernoulli(prob = aci)
            val sendOrNot: Boolean
            if (predictionOfStatusCode != 400) {
                sendOrNot = true
            }else{
                sendOrNot = if(Math.random() > classifier.performance.estimateAccuracy()) true else false
            }

            // Execute the request and update
            if (sendOrNot) {
                val result = executeRestCallAction(action,"$baseUrlOfSut")
                println("Response     : ${result.getStatusCode()}")
                println("Updating the $modelName classifier!")
                classifier.updateModel(action, result)

                if (classifier is GaussianModel) {

                    val d200 = classifier.density200!!
                    val d400 = classifier.density400!!

                    fun formatStats(name: String, mean: List<Double>, variance: List<Double>, n: Int) {
                        val m = mean.map { "%.2f".format(it) }
                        val v = variance.map { "%.2f".format(it) }
                        println("$name: n=$n, mean=$m, variance=$v * I_${classifier.dimension}")
                    }

                    formatStats("Density200", d200.mean, d200.variance, d200.n)
                    formatStats("Density400", d400.mean, d400.variance, d400.n)

                }else if (classifier is GLMModel){
                    println("Updating the $modelName classifier!")
                    println("Weights and Bias = ${classifier.getModelParams()}")
                }

            }

        }
    }

}
