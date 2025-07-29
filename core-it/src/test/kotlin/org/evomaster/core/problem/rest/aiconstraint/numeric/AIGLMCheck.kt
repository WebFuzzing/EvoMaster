package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICMultiTypeController
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
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.core.MediaType


class AIGLMCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(AICMultiTypeController())
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
            result.setBodyType(MediaType.APPLICATION_JSON_TYPE) // or guess based on Content-Type header

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
                val g = p.gene
                g !is IntegerGene && g !is DoubleGene && g !is BooleanGene && g !is EnumGene<*>
            }

            val dimension = if (hasUnsupportedGene) {
                null
            } else {
                action.parameters.count { p ->
                    val g = p.gene
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
        var time = 1
        val timeLimit = 200
        while (time <= timeLimit) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val name = sampledAction.getName()
            val classifier = endpointToClassifier[name]
            val dimension = endpointToDimension[name]
            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }

            println("**********************************************")
            println("Time         : $time")
            println("Path         : $name")
            println("Classifier   : ${if (classifier == null) "null" else "GLM"}")
            println("Dimension    : $dimension")
            println("Input Genes  : ${geneValues.joinToString(", ")}")
            println("Actual Genes : ${geneValues.size}")

            //  executeRestCallAction is replaced with createIndividual to avoid override error
            //  val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val individual = sampler.createIndividual(SampleType.RANDOM, listOf(sampledAction).toMutableList())
            val action = individual.seeMainExecutableActions()[0]
            val result = executeRestCallAction(action,"$baseUrlOfSut")
            println("Response     : ${result.getStatusCode()}")

            // Skip classification for the endpoints with unsupported genes
            if (classifier==null){
                println("No classification as the classifier is null, i.e., the endpoint contains unsupported genes")
                continue
            }

            // Update and classify
            classifier.updateModel(action, result)
            val classification = classifier.classify(action)

            println("Probabilities: ${classification.scores}")
            require(classification.scores.values.all { it in 0.0..1.0 }) {
                "In GLM, scores must be in [0,1]"
            }

            if (classifier != null) {
                val weightsAndBias = classifier.getModelParams()
                println("Weights and Bias = $weightsAndBias")
                println("**********************************************")
            } else {
                println("The classifier is not a GLMOnlineClassifier")
            }
            time++
        }
    }
}
