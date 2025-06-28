package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICMultiTypeController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.classifier.GLMOnlineClassifier
import org.evomaster.core.problem.rest.data.RestPath
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

        val pathToDimension = mutableMapOf<RestPath, Int>()
        for (action in actionList) {
            val path = action.path
            if (pathToDimension.containsKey(path)) continue

            val dimension = action.parameters.count { p ->
                val g = p.gene
                g is IntegerGene || g is DoubleGene || g is BooleanGene || g is EnumGene<*>
            }
            pathToDimension[path] = dimension
        }

        val pathToClassifier = mutableMapOf<RestPath, GLMOnlineClassifier>()
        for ((path, dimension) in pathToDimension) {
            val model = GLMOnlineClassifier()
            model.setDimension(dimension)
            pathToClassifier[path] = model
        }

        println("Classifiers initialized with their dimensions:")
        for ((path, expected) in pathToDimension) {
            val classifier = pathToClassifier[path]!!
            println("$path -> expected: $expected, actualDim: ${classifier.getDimension()}")
        }


        val random = Randomness()
        val sampler = injector.getInstance(AbstractRestSampler::class.java)
        var time = 1
        val timeLimit = 20
        while (time <= timeLimit) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val path = sampledAction.path
            val dimension = pathToDimension[path] ?: error("No dimension for path: $path")
            val classifier = pathToClassifier[path] ?: error("Expected classifier for path: $path")
            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }

            println("*************************************************")
            println("Time         : $time")
            println("Path         : $path")
            println("Input Genes  : ${geneValues.joinToString(", ")}")
            println("Input dim    : ${classifier.getDimension()}")
            println("Expected Dim : $dimension")
            println("Actual Genes : ${geneValues.size}")

            //  //executeRestCallAction is replaced with createIndividual to avoid override error
            //  val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val individual = sampler.createIndividual(SampleType.RANDOM, listOf(sampledAction).toMutableList())
            val action = individual.seeMainExecutableActions()[0]
            val result = executeRestCallAction(action,"$baseUrlOfSut")
            println("Response:\n${result.getStatusCode()}")


            // Update and classify
            classifier.updateModel(action, result)
            val classification = classifier.classify(action)

            println("Probabilities: ${classification.probabilities}")
            require(classification.probabilities.values.all { it in 0.0..1.0 }) {
                "All probabilities must be in [0,1]"
            }

            if (classifier != null) {
                val weightsAndBias = classifier.getModelParams()
                println("Weights and Bias = $weightsAndBias")
                println("**********************************************")
                println("**********************************************")
            } else {
                println("The classifier is not a GLMOnlineClassifier")
            }
            time++
        }
    }
}
