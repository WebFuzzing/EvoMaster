package org.evomaster.core.problem.rest.aiclassification

import bar.examples.it.spring.aiclassification.basic.BasicController
import bar.examples.it.spring.aiclassification.multitype.MultiTypeController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.GLMOnlineClassifier
import org.evomaster.core.problem.rest.classifier.InputEncoderUtils
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType
import kotlin.collections.iterator
import kotlin.math.abs


class AIGLMCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
//            initClass(BasicController())
            initClass(MultiTypeController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIGLMCheck()
            init()
            test.initializeTest()
            test.runClassifierExample()
        }
    }

    fun initializeTest() {
        recreateInjectorForWhite(
            listOf(
                "--aiModelForResponseClassification", "GLM",
                "--aiResponseClassifierLearningRate", "0.05"
            )
        )
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

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val config = EMConfig().apply {
            aiModelForResponseClassification = EMConfig.AIResponseClassifierModel.GLM
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

            val hasUnsupportedGene = action.parameters.any { p ->
                val g = p.gene.getLeafGene()
                g !is IntegerGene && g !is DoubleGene && g !is BooleanGene && g !is EnumGene<*>
            }

            val dimension = if (hasUnsupportedGene) {
                null
            } else {
                action.parameters.count { p ->
                    val g = p.gene.getLeafGene()
                    g is IntegerGene || g is DoubleGene || g is BooleanGene || g is EnumGene<*>
                }
            }

            println("Endpoint: $name, dimension: $dimension")
            endpointToDimension[name] = dimension

        }

        /**
         * Initialize a classifier for each endpoint
         * For an endpoint containing unsupported genes, the associated classifier is null
         */
        val endpointToClassifier = mutableMapOf<String, GLMOnlineClassifier?>()
        for ((name, dimension) in endpointToDimension) {
            if(dimension==null){
                endpointToClassifier[name] = null
            }else{
                val model = GLMOnlineClassifier()
                model.setDimension(dimension)
                endpointToClassifier[name] = model
            }
        }

        for ((name, expectedDimension) in endpointToDimension) {
            println("Expected dimension for $name: $expectedDimension")
        }

        // Execute the procedure for a period of time
        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)
        val startTime = System.currentTimeMillis()
        val runDuration = 5_000L // Milliseconds
        while (System.currentTimeMillis() - startTime < runDuration) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val name = sampledAction.getName()
            val classifier = endpointToClassifier[name]
            val dimension = endpointToDimension[name]
            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }

            println("*************************************************")
            println("Path         : $name")
            println("Classifier   : ${if (classifier == null) "null" else "GLM"}")
            println("Dimension    : $dimension")
            println("Input Genes  : ${geneValues.joinToString(", ")}")
            println("Genes Size   : ${geneValues.size}")
            println("cp, tot, ac  : ${classifier?.getCorrectPredictions()}, ${classifier?.getTotalRequests()}, ${classifier?.getAccuracy()}")

            //  executeRestCallAction is replaced with createIndividual to avoid override error
            //  val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val individual = sampler.createIndividual(SampleType.RANDOM, listOf(sampledAction).toMutableList())
            val action = individual.seeMainExecutableActions()[0]

            // Execution without classification for the endpoints with unsupported genes
            if (classifier==null){
                executeRestCallAction(action,"$baseUrlOfSut")
                println("No classification as the classifier is null, i.e., the endpoint contains unsupported genes")
                continue
            }
            // Warmup cold classifiers by at least n request
            val n = 2
            val isCold = classifier.getTotalRequests() <= n
            if (isCold) {
                println("Warmup by at least $n request")
                val result = executeRestCallAction(action, "$baseUrlOfSut")
                classifier.updateModel(action, result)
                classifier.setTot(classifier.getTotalRequests() + 1)
                continue
            }

            // Classification
            println("Classifying!")
            val rawEncodedFeatures = InputEncoderUtils.encode(sampledAction).rawEncodedFeatures
            println("Raw encoded features are : ${rawEncodedFeatures.joinToString(", ")}")
            val classification = classifier.classify(action)
            val p200 = classification.probabilities[200]!!
            val p400 = classification.probabilities[400]!!
            require(p200 in 0.0..1.0 && p400 in 0.0..1.0 && abs((p200 + p400) - 1.0) < 1e-6) {
                "Probabilities must be in [0,1] and sum to 1"
            }

            // Prediction
            val prediction: Int = if (p200 > p400) 200 else 400
            println("Prediction is : $prediction")

            // Probabilistic decision-making based on Bernoulli(prob = aci)
            val sendOrNot: Boolean
            if (prediction != 400) {
                sendOrNot = true
            }else{
                sendOrNot = if(Math.random() > classifier.getAccuracy()) true else false
            }

            // Execute the request and update
            if (sendOrNot) {
                val result = executeRestCallAction(action,"$baseUrlOfSut")
                println("Response     : ${result.getStatusCode()}")

                if (result.getStatusCode()==prediction) {
                    classifier.setCp(classifier.getCorrectPredictions() + 1)
                }
                classifier.setTot(classifier.getTotalRequests() + 1)

                println("Updating the classifier!")
                classifier.updateModel(action, result)
            }

            println("Weights and Bias = ${classifier.getModelParams()}")
            println("**********************************************")

        }
    }
}
