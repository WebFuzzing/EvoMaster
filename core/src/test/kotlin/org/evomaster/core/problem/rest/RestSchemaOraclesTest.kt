package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.search.FitnessValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RestSchemaOraclesTest{

    @Test
    fun testNoIssuesInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()

        val oracles = RestSchemaOracles(schema)

        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)
        response.setStatusCode(401)

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        assertFalse(report.hasErrors(), "Expecting no errors, but got: $report")
        //TODO other checks
    }


    @Test
    fun testWrongStatusInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()

        val oracles = RestSchemaOracles(schema)

        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)
        response.setStatusCode(204) // not declared

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        assertTrue(report.hasErrors())
        assertEquals(1, report.messages.count())
        //TODO other checks
    }

}