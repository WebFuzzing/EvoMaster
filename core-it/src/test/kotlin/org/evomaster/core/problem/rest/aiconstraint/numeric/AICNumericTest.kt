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
}