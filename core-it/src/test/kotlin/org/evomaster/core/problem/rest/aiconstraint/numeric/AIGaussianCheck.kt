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
import org.evomaster.core.problem.rest.classifier.GaussianOnlineClassifier
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.service.Randomness


class AIGaussianCheck : IntegrationTestRestBase() {

    companion object {
        @JvmStatic
        fun init() {
            initClass(AICMultiTypeController())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val test = AIGaussianCheck()
            init()
            test.initializeTest()
            test.runClassifierExample()
        }
    }

    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", "GAUSSIAN"))
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

        // Step 1: Compute dimensions
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

        // Step 2: Initialize classifiers
        val pathToClassifier = mutableMapOf<String, GaussianOnlineClassifier>()
        for ((path, dimension) in pathToDimension) {
            val classifier = injector.getInstance(AIResponseClassifier::class.java)
            classifier.initModel()

            val delegateField = classifier::class.java.getDeclaredField("delegate")
            delegateField.isAccessible = true
            val gaussian = delegateField.get(classifier) as? GaussianOnlineClassifier
                ?: throw IllegalStateException("Delegate is not a GaussianOnlineClassifier")

            gaussian.setDimension(dimension)
            pathToClassifier[path] = gaussian
        }

        // Print for verification
        println("Classifiers initialized with their dimensions:")
        for ((path, expected) in pathToDimension) {
            val actualDim = pathToClassifier[path]?.getDimension()
            println("$path -> expected: $expected, actualDim: $actualDim")
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
            val gaussianTemp = pathToClassifier[path] ?: error("No classifier for path: $path")

            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }

            println("*************************************************")
            println("*************************************************")
            println("*************************************************")
            println("*************************************************")
            println("Time         : $time")
            println("Path         : $path")
            println("gaussian dim : ${gaussianTemp.getDimension()}")
            println("Input Genes  : ${geneValues.joinToString(", ")}")
            println("Expected Dim : $dimension")
            println("Actual Genes : ${geneValues.size}")

            if (geneValues.size != dimension) {
                println("Gene count ${geneValues.size} != expected dimension $dimension")
            }

            // Evaluate and classify
            val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val evaluatedAction = individual.evaluatedMainActions()[0]
            val action = evaluatedAction.action as RestCallAction
            val result = evaluatedAction.result as RestCallResult

            gaussianTemp.updateModel(action, result)
            val classification = gaussianTemp.classify(action)

            println("Probabilities: ${classification.probabilities}")
            // the classification provides two values as the probability of getting 400 and 200
            require(classification.probabilities.values.all { it in 0.0..1.0 }) {
                "All probabilities must be in [0,1]"
            }

            val d200 = gaussianTemp.getDensity200()
            val d400 = gaussianTemp.getDensity400()

            fun formatStats(name: String, mean: List<Double>, variance: List<Double>, n: Int) {
                val m = mean.map { "%.2f".format(it) }
                val v = variance.map { "%.2f".format(it) }
                println("$name: n=$n, mean=$m, variance=$v * I_$dimension")
            }

            formatStats("Density200", d200.mean, d200.variance, d200.n)
            formatStats("Density400", d400.mean, d400.variance, d400.n)

            println("----------------------------------")
            time++
        }

    }
}