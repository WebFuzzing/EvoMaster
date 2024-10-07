package org.evomaster.core.problem.rest

import com.atlassian.oai.validator.report.ValidationReport
import io.swagger.parser.OpenAPIParser
import org.evomaster.core.search.FitnessValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

class RestSchemaOraclesTest{


    private fun checkNoError(report: ValidationReport){
        assertFalse(report.hasErrors(), "Expecting no errors, but got: $report")
    }

    @Test
    fun testNoIssuesInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()

        val oracles = RestSchemaOracles(schema)

        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)
        response.setStatusCode(401)

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        checkNoError(report)
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

    @Test
    fun testValidBodyInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracles(schema)
        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)
        response.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        response.setBody("""
            {"value": 42}
        """.trimIndent())

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        checkNoError(report)
        //TODO other checks

    }


    @Test
    fun testMissingBodyInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracles(schema)
        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)


        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        assertTrue(report.hasErrors())
        assertEquals(1, report.messages.count())
        //TODO other checks
    }


    /*
        FIXME
        library is buggy:
        https://bitbucket.org/atlassian/swagger-request-validator/issues/369/make-the
        additionalProperties are allowed by default, unless explicitly disabled
     */
    @Disabled
    @Test
    fun testExtraParamBodyInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracles(schema)
        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)
        response.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        response.setBody("""
            {
                "value": 42,
                "x": "foo"
            }
        """.trimIndent())

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response, fv)
        checkNoError(report)
        //TODO other checks
    }

    @Test
    fun testRequiredParam(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/openapi-validation.yaml").readText()
        val oracles = RestSchemaOracles(schema)
        val fv = FitnessValue(1.0)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)

        val report = oracles.handleSchemaOracles("/api/x", HttpVerb.GET, response, fv)
        //no check on inputs (ie robustness testing), but only responses
        checkNoError(report)
    }
}