package org.evomaster.core.problem.rest

import com.atlassian.oai.validator.report.ValidationReport
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

        val oracles = RestSchemaOracle(schema)

        val response = RestCallResult("id",false)
        response.setStatusCode(401)

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response)
        checkNoError(report)
    }


    @Test
    fun testWrongStatusInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracle(schema)
        val response = RestCallResult("id",false)

        response.setStatusCode(204) // not declared

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response)
        assertTrue(report.hasErrors())
        assertEquals(1, report.messages.count())
    }

    @Test
    fun testValidBodyInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracle(schema)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)
        response.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        response.setBody("""
            {"value": 42}
        """.trimIndent())

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response)
        checkNoError(report)
    }


    @Test
    fun testMissingBodyInBranches(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/branches.json").readText()
        val oracles = RestSchemaOracle(schema)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)


        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response)
        assertTrue(report.hasErrors())
        assertEquals(1, report.messages.count())
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
        val oracles = RestSchemaOracle(schema)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)
        response.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        response.setBody("""
            {
                "value": 42,
                "x": "foo"
            }
        """.trimIndent())

        val report = oracles.handleSchemaOracles("/api/branches/eq", HttpVerb.POST, response)
        checkNoError(report)
    }

    @Test
    fun testRequiredParam(){

        val schema = RestSchemaOraclesTest::class.java.getResource("/swagger/artificial/openapi-validation.yaml").readText()
        val oracles = RestSchemaOracle(schema)
        val response = RestCallResult("id",false)

        response.setStatusCode(200)

        val report = oracles.handleSchemaOracles("/api/x", HttpVerb.GET, response)
        //no check on inputs (ie robustness testing), but only responses
        checkNoError(report)
    }
}