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
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification","GAUSSIAN"))
    }

    fun runClassifierExample() {

        /**
         * Generate a random RestCallAction using EvoMaster Randomness
         */
        // Fetch and parse OpenAPI schema based on the schema location
        val schema = OpenApiAccess.getOpenAPIFromLocation("$baseUrlOfSut/v3/api-docs")
        // Wrap schema into RestSchema
        val restSchema = RestSchema(schema)
        // Configuration for gene generation
        val config = EMConfig().apply {
            enableSchemaConstraintHandling = true
            allowInvalidData = false
            probRestDefault = 0.0
            probRestExamples = 0.0
        }
        val options = RestActionBuilderV3.Options(config)
        // actionCluster contains provides possible actions
        val actionCluster = mutableMapOf<String, Action>()
        // Generate RestCallAction
        RestActionBuilderV3.addActionsFromSwagger(restSchema, actionCluster, options = options)
        // Sample a random RestCallAction
        val random = Randomness()
        val actionList = actionCluster.values.filterIsInstance<RestCallAction>()
        val template = random.choose(actionList)
        val sampledAction = template.copy() as RestCallAction
        sampledAction.doInitialize(random)

        // Calculate the input dimension of the classifier
        var dimension:Int = 0
        for (gene in sampledAction.seeTopGenes()) {
            when (gene) {
                is IntegerGene, is DoubleGene, is BooleanGene, is EnumGene<*> -> {
                    dimension++
                }
            }
        }
        //require(dimension == 6) //FIXME

        // Create a gaussian classifier
        val classifier = injector.getInstance(AIResponseClassifier::class.java)
        //classifier.initModel(dimension) //FIXME

        // Use reflection to access the private delegate
        val delegateField = classifier::class.java.getDeclaredField("delegate")
        delegateField.isAccessible = true
        val gaussian = delegateField.get(classifier) as? GaussianOnlineClassifier

        var time =1
        val timeLimit = 20
        while (time <= timeLimit) {
            val template = random.choose(actionList)
            val sampledAction = template.copy() as RestCallAction
            sampledAction.doInitialize(random)

            val geneValues = sampledAction.parameters.map { it.gene.getValueAsRawString() }
            println("**********************************************")
            println("Time: $time")
            println("Genes: [${geneValues.joinToString(", ")}]")

            // createIndividual send the request and evaluate
            val individual = createIndividual(listOf(sampledAction), SampleType.RANDOM)
            val evaluatedAction = individual.evaluatedMainActions()[0]
            val action = evaluatedAction.action as RestCallAction
            val result = evaluatedAction.result as RestCallResult

            // update the model
            classifier.updateModel(action, result)

            // classify an action
            val c = classifier.classify(action)
            // the classification provides two values as the probability of getting 400 and 200
            require(c.probabilities.values.all { it in 0.0..1.0 }) {
                "All probabilities must be in [0,1]"
            }

            if (gaussian != null) {
                val d200 = gaussian.getDensity200()
                val d400 = gaussian.getDensity400()
                val mean200 = d200.mean.map { "%.2f".format(it) }
                val var200 = d200.variance.map { "%.2f".format(it) }
                val mean400 = d400.mean.map { "%.2f".format(it) }
                val var400 = d400.variance.map { "%.2f".format(it) }

                println(
                    """
                    n200 = ${d200.n}
                    mean200 = $mean200
                    variance200 = $var200 * I_$dimension
                    n400 = ${d400.n}
                    mean400 = $mean400
                    variance400 = $var400 * I_$dimension
                    """.trimIndent()
                )
                println("**********************************************")
            } else {
                println("The classifier is not a GaussianOnlineClassifier")
            }


            time++
        }


    }

}

