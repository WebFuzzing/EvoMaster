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
import kotlin.math.*


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
        val initialMean = Random.nextDouble(0.0, 5000.0)
        val model = NaiveGaussianModel1D(initialMean = initialMean, initialVariance = 10.0)
        val numIterations = 5000
        var result: Int? = 0;
        repeat(numIterations) {
            val sample = model.sample()
            val xValue = sample.toInt().coerceIn(0, 5000)

            val get = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to xValue.toString()))!!
            val individual = createIndividual(listOf(get), SampleType.RANDOM)
            result = (individual.evaluatedMainActions()[0].result as RestCallResult).getStatusCode()

            println("Sampled x=$xValue, got status=$result")

            if (result == 200) {
                model.updateAccepted(xValue.toDouble())
            } else if (result == 400) {
                model.updateRejected(xValue.toDouble())
            }
        }

        println("Final mean: ${model.mean()}, variance: ${model.variance()}")
        assertEquals(result, 200)
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
