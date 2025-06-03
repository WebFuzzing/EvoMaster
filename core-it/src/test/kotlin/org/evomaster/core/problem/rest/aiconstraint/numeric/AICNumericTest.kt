package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICNumericController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.service.AIResponseClassifier
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.random.Random


class AICNumericTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AICNumericController())
        }
    }

    @BeforeEach
    fun initializeTest() {
        recreateInjectorForWhite(listOf("--aiModelForResponseClassification", "NAIVE_GAUSSIAN_1D"))
    }

    @Test
    fun testBasicInjectorCallModelOnce_400() {

        val pirTest = getPirToRest()
        // get is a RestCallAction
        val getRestCallAction = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to "2026.0"))!!

        // createIndividual send the request and evaluate, while we want to predict before sending
        val individual = createIndividual(listOf(getRestCallAction), SampleType.RANDOM)
        val evaluatedAction = individual.evaluatedMainActions()[0]
        val action = evaluatedAction.action as RestCallAction
        val result = evaluatedAction.result as RestCallResult
        assertEquals(400, result.getStatusCode())

        val classifier = injector.getInstance(AIResponseClassifier::class.java)
        classifier.updateModel(action, result)
        val classificationResult = classifier.classify(action)
//        assertTrue(c.probabilityOf400() == 0.0)
        assertTrue(classificationResult.probabilityOf400() > classificationResult.probabilityOf200())
    }

    @Test
    fun testBasicInjectorCallModelOnce_200() {

        val pirTest = getPirToRest()
        // get is a RestCallAction
        val getRestCallAction = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to "2025.0"))!!

        // createIndividual send the request and evaluate, while we want to predict before sending
        val individual = createIndividual(listOf(getRestCallAction), SampleType.RANDOM)
        val evaluatedAction = individual.evaluatedMainActions()[0]
        val action = evaluatedAction.action as RestCallAction
        val result = evaluatedAction.result as RestCallResult
        assertEquals(200, result.getStatusCode())

        val classifier = injector.getInstance(AIResponseClassifier::class.java)
        classifier.updateModel(action, result)
        val classificationResult = classifier.classify(action)
//        assertTrue(c.probabilityOf400() == 0.0)
        assertTrue(classificationResult.probabilityOf400() < classificationResult.probabilityOf200())
    }


    @Test
    fun testAIModel() {

        val classifier = injector.getInstance(AIResponseClassifier::class.java)


        // The learning continues until we reach the time limit
        val timeLimit: Int = 100
        var time: Int = 1
        while (time < timeLimit) {

            val pirTest = getPirToRest()

            // Create a randomRestCallAction
            // This part must be replaced by evomaster random generator!
            val randomNum = Random.nextDouble()
            // Here getTemp is a RestCallAction
            val getTemp = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to "$randomNum"))!!

            // predict the response by the classifier
            val c = classifier.classify(getTemp)


            // Strict approach
            // Send the request if the classifier says the request is valid
            // This approach is very biased!
//            if (c == StatusGroup.G_2xx) { //TODO put back
//                // createIndividual function create and test an individual
//                val individual = createIndividual(listOf(getTemp), SampleType.RANDOM)
//                val evaluatedAction = individual.evaluatedMainActions()[0]
//                val action = evaluatedAction.action as RestCallAction
//                val result = evaluatedAction.result as RestCallResult
//
//                // update the classifier based on the response
//                classifier.updateModel(action, result)
//
//
//            }

            time += 1

        }

    }

    @Test
    fun learnValidNumberRangeUsingNaiveGaussian() {
        val pirTest = getPirToRest()
        val lowerBound = -5_000
        val upperBound = 5_000

        val initialMean: Int = try {
            findInitialValidInput(lowerBound, upperBound, 10_000)
        } catch (e: Exception) {
            Random.nextInt(lowerBound, upperBound)
        }

        val model = NaiveGaussianModel1D(initialMean = initialMean, initialVariance = 10.0, lowerBound, upperBound)

        val numIterations = 2_000
        var result: Int? = 0;

        repeat(numIterations) {
            val xValue = model.sample()
            val get = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to xValue.toString()))!!
            val individual = createIndividual(listOf(get), SampleType.RANDOM)
            result = (individual.evaluatedMainActions()[0].result as RestCallResult).getStatusCode()

            println("Sampled x=$xValue, got status=$result")

            val csvFile = File("samples.csv")
            if (it == 0) {
                csvFile.writeText("x,result\n") // header on first iteration
            }
            csvFile.appendText("$xValue,$result\n")

            if (result == 200) {
                model.updateAccepted(xValue)
            } else if (result == 400) {
                model.updateRejected(xValue)
            }
        }

        println("Final mean: ${model.mean()}, variance: ${model.variance()}")
        assertEquals(result, 200)
    }

    private fun findInitialValidInput(
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        maxAttempts: Int = 10_000
    ): Int {
        val pirTest = getPirToRest()
        val rng = Random.Default

        repeat(maxAttempts) {
            val guess = rng.nextInt(min, max + 1)

            val get = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to guess.toString()))!!
            val individual = createIndividual(listOf(get), SampleType.RANDOM)
            val result = (individual.evaluatedMainActions()[0].result as RestCallResult).getStatusCode()

            println("Trying initial x=$guess, got status=$result")
            if (result == 200) return guess
        }

        error("Failed to find a valid initial value after $maxAttempts attempts")
    }
}