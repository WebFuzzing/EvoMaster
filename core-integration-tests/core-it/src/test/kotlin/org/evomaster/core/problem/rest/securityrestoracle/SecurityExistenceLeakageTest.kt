package org.evomaster.core.problem.rest.securityrestoracle

import bar.examples.it.spring.existenceleakage.ExistenceLeakageApplication
import bar.examples.it.spring.existenceleakage.ExistenceLeakageController
import org.evomaster.core.JdkIssue
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.oracle.RestSecurityOracle
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.problem.rest.service.RestIndividualBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecurityExistenceLeakageTest: IntegrationTestRestBase()  {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            JdkIssue.fixPatchMethod()

            initClass(ExistenceLeakageController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        ExistenceLeakageApplication.reset()
        getEMConfig().security = true
        getEMConfig().schemaOracles = false
    }

    @Test
    fun testLeakage(){

        val pirTest = getPirToRest()

        val id42 = 42
        val id123 = 123

        val put42 = pirTest.fromVerbPath("PUT", "/api/resources/$id42")!!
        val get42 = pirTest.fromVerbPath("GET", "/api/resources/$id42")!!
        val get123 = pirTest.fromVerbPath("GET", "/api/resources/$id123")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)

        put42.auth = foo
        get42.auth = bar
        get123.auth = bar


        val forbidden = createIndividual(listOf(put42,get42), SampleType.SECURITY)
        assertEquals(201, (forbidden.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (forbidden.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())

        val notFound = createIndividual(listOf(get123), SampleType.SECURITY)
        assertEquals(404, (notFound.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())

        val ind = RestIndividualBuilder.merge(forbidden.individual, notFound.individual)
        assertEquals(HttpVerb.PUT,  ind.seeMainExecutableActions()[0].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[1].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[2].verb)

        ExistenceLeakageApplication.reset()
        val ff = injector.getInstance(AbstractRestFitness::class.java)
        val ei = ff.calculateCoverage(ind)!!

        val r0 = ei.evaluatedMainActions()[0].result as RestCallResult
        val r1 = ei.evaluatedMainActions()[1].result as RestCallResult
        val r2 = ei.evaluatedMainActions()[2].result as RestCallResult
        assertEquals(201, r0.getStatusCode())
        assertEquals(403, r1.getStatusCode())
        assertEquals(404, r2.getStatusCode())

        val faultDetected = RestSecurityOracle.hasExistenceLeakage(RestPath("/api/resources/{id}"),ei.individual, ei.seeResults())
        assertTrue(faultDetected)

        //fault should be put on 404
        assertEquals(0, r0.getFaults().size)
        assertEquals(0, r1.getFaults().size)
        assertEquals(1, r2.getFaults().size)
    }

}