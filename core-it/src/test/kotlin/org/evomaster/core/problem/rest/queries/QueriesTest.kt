package org.evomaster.core.problem.rest.queries


import bar.examples.it.spring.queries.QueriesController
import org.evomaster.core.problem.rest.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class QueriesTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(QueriesController())
        }
    }

    @Test
    fun testStringOK(){

        val pirTest = getPirToRest()

        val get = pirTest.fromVerbPath("get", "/api/queries/string", mapOf("x" to "FOO", "y" to "BAR"))!!

        val x = createIndividual(listOf(get))
        val res = x.evaluatedMainActions()[0].result as RestCallResult
        assertEquals(200, res.getStatusCode())
    }

    //TODO write more tests for different combinations
}