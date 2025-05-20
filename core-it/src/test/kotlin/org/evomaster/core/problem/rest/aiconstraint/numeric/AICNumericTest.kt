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

class AICNumericTest: IntegrationTestRestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AICNumericController())
        }
    }

    @BeforeEach
    fun initializeTest(){
    }


    @Test
    fun testNumeric() {


        val pirTest = getPirToRest()
        val get = pirTest.fromVerbPath("get", "/api/numeric", mapOf("x" to "5"))!!

        val x = createIndividual(listOf(get), SampleType.RANDOM)
        val evaluatedAction = x.evaluatedMainActions()[0]
        val action = evaluatedAction.action as RestCallAction
        val result = evaluatedAction.result as RestCallResult
        assertEquals(200, result.getStatusCode())

        //injector.getInstance() //TODO get reference to the model singleton
    }

    @Test
    fun learnValidNumberRangeUsingNaiveGaussian() {
        val pirTest = getPirToRest()
        val initialMean = Random.nextDouble(0.0, 1000.0)
        val model = NaiveGaussianModel1D(initialMean = initialMean, initialVariance = 10.0)
        val numIterations = 5000
        var result: Int? = 0;
        repeat(numIterations) {
            val sample = model.sample()
            val xValue = sample.toInt().coerceIn(0, 1000)

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

}