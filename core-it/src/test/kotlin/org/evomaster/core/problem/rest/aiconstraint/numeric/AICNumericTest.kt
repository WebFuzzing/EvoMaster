package org.evomaster.core.problem.rest.aiconstraint.numeric

import bar.examples.it.spring.aiconstraint.numeric.AICNumericController
import bar.examples.it.spring.cleanupcreate.CleanUpDeleteApplication
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random
import java.io.File


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
    }

    @Test
    fun testNumeric() {


        val pirTest = getPirToRest()
        val get = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to "5"))!!

        val individual0 = createIndividual(listOf(get), SampleType.RANDOM)
        val evaluatedAction = individual0.evaluatedMainActions()[0]
        val action = evaluatedAction.action as RestCallAction
        val result = evaluatedAction.result as RestCallResult
        assertEquals(200, result.getStatusCode())
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
            }
            else if (result == 400) {
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


//    fun testGaussianModel() {
//        val model = NaiveGaussianModel1D(initialMean = 1950.0, initialVariance = 1000.0)
//        val checker = CriteriaChecker()
//
//        repeat(100) {
//            val valList = model.generateRandomNumbers(1)
//            val value = valList[0]
//            if (checker.check(value) == 400) {
//                println("The sample is Invalid.")
//            } else {
//                println("The sample is valid.")
//                model.update(value)
//                println(model.values)
//                println("The model is updated.")
//            }
//        }
//    }
//
//    class NaiveGaussianModel1D(initialMean: Double = 0.0, initialVariance: Double = 1.0) {
//        private var n: Int = 1
//        private var mu: Double = initialMean
//        private var M2: Double = initialVariance
//        val values: MutableList<Double> = mutableListOf(initialMean)
//
//        fun update(x: Double) {
//            values.add(x)
//            n += 1
//            val delta = x - mu
//            mu += delta / n
//            val delta2 = x - mu
//            M2 += delta * delta2
//        }
//
//        fun posteriorMean(): Double = mu
//
//        fun posteriorVariance(): Double = if (n > 1) M2 / (n - 1) else 1e-6
//
//        fun generateRandomNumbers(numberOfSamples: Int = 1): List<Double> {
//            val mean = posteriorMean()
//            val stddev = sqrt(posteriorVariance())
//            return List(numberOfSamples) {
//                val random = Random()
//                random.nextGaussian() * stddev + mean
//            }
//        }
//    }
//
//    class CriteriaChecker {
//        fun check(x: Double): Int {
//            return if (x in 1925.0..2025.0) 200 else 400
//        }
//    }
}
