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
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness


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

    fun runClassifierExample() {
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        val restSchema = RestSchema(schema)

        val config = EMConfig().apply {
            enableSchemaConstraintHandling = true
            allowInvalidData = false
            probRestDefault = 0.0
            probRestExamples = 0.0
        }
        val options = RestActionBuilderV3.Options(config)
        val actionCluster = mutableMapOf<String, Action>()
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)

        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()
        val pathToDimension = mutableMapOf<String, Int>()
        for (action in actionList) {
            val path = action.path.toString()
            if (pathToDimension.containsKey(path)) continue

            val dimension = action.parameters.count { p ->
                val g = p.gene
                g is IntegerGene || g is DoubleGene || g is BooleanGene || g is EnumGene<*>
            }
            pathToDimension[path] = dimension
        }

        val pathToClassifier = mutableMapOf<String, AIResponseClassifier>()
        for ((path, dimension) in pathToDimension) {
            val glm = GLMOnlineClassifier(learningRate = 0.05)
            glm.setDimension(dimension)
            val classifier = AIResponseClassifier(explicitModel = glm)
            pathToClassifier[path] = classifier
        }

        val random = Randomness()
        var time = 1
        val timeLimit = 20
        while (time <= timeLimit) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val path = sampledAction.path.toString()
            val dimension = pathToDimension[path] ?: error("No dimension for path: $path")
            val classifier = pathToClassifier[path] ?: error("No classifier for path: $path")
            val glm = classifier.getInnerModel() as? GLMOnlineClassifier

            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }
            println("**********************************************")
            println("Time: $time")
            println("Path: $path")
            println("Genes: [${geneValues.joinToString(", ")}]")

            val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val evaluatedAction = individual.evaluatedMainActions()[0]
            val action = evaluatedAction.action as RestCallAction
            val result = evaluatedAction.result as RestCallResult

            classifier.updateModel(action, result)
            val classification = classifier.classify(action)
            require(classification.probabilities.values.all { it in 0.0..1.0 }) {
                "All probabilities must be in [0,1]"
            }

            if (glm != null) {
                val weightsAndBias = glm.getModelParams()
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
