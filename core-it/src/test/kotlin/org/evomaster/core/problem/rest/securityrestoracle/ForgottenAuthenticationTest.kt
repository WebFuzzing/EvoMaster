package org.evomaster.core.problem.rest.securityrestoracle

import bar.examples.it.spring.forgottenauthentication.ForgottenAuthenticationApplication
import bar.examples.it.spring.forgottenauthentication.ForgottenAuthenticationController
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ForgottenAuthenticationTest: IntegrationTestRestBase()  {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            JdkIssue.fixPatchMethod()

            initClass(ForgottenAuthenticationController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        ForgottenAuthenticationApplication.reset()
        getEMConfig().security = true
        getEMConfig().schemaOracles = false
    }

    @Test
    fun testForgottenAuthentication(){

        val pirTest = getPirToRest()

        val id42 = 42

        val put42 = pirTest.fromVerbPath("PUT", "/api/resources/$id42")!!
        val get42 = pirTest.fromVerbPath("GET", "/api/resources/$id42")!!
        val get42NotAuth = pirTest.fromVerbPath("GET", "/api/resources/$id42")!!
        val get42DifferentAuth = pirTest.fromVerbPath("GET", "/api/resources/$id42")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)

        put42.auth = foo
        get42.auth = foo
        get42DifferentAuth.auth = bar


        val authenticated = createIndividual(listOf(put42,get42,get42DifferentAuth), SampleType.SECURITY)
        assertEquals(201, (authenticated.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(200, (authenticated.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())
        assertEquals(403, (authenticated.evaluatedMainActions()[2].result as RestCallResult).getStatusCode())

        val forgottenAuth = createIndividual(listOf(get42NotAuth), SampleType.SECURITY)
        assertEquals(200, (forgottenAuth.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())

        val ind = RestIndividualBuilder.merge(authenticated.individual, forgottenAuth.individual)
        assertEquals(HttpVerb.PUT,  ind.seeMainExecutableActions()[0].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[1].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[2].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[3].verb)

        ForgottenAuthenticationApplication.reset()
        val ff = injector.getInstance(AbstractRestFitness::class.java)
        val ei = ff.calculateCoverage(ind)!!

        val r0 = ei.evaluatedMainActions()[0].result as RestCallResult
        val r1 = ei.evaluatedMainActions()[1].result as RestCallResult
        val r2 = ei.evaluatedMainActions()[2].result as RestCallResult
        val r3 = ei.evaluatedMainActions()[3].result as RestCallResult
        assertEquals(201, r0.getStatusCode())
        assertEquals(200, r1.getStatusCode())
        assertEquals(403, r2.getStatusCode())
        assertEquals(200, r3.getStatusCode())

        val faultDetected = RestSecurityOracle.hasForgottenAuthentication(get42NotAuth.getName(), ei.individual, ei.seeResults())
        assertTrue(faultDetected)

        //fault should be put on 200 with no authentication
        assertEquals(0, r0.getFaults().size)
        assertEquals(0, r1.getFaults().size)
        assertEquals(0, r2.getFaults().size)
        assertEquals(1, r3.getFaults().size)
    }


    @Test
    fun testForgottenAuthenticationWithCreateAndNoAuthRequest(){

        val pirTest = getPirToRest()

        val id42 = 42

        val put42 = pirTest.fromVerbPath("PUT", "/api/resources/$id42")!!
        val get42NotAuth = pirTest.fromVerbPath("GET", "/api/resources/$id42")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)

        put42.auth = foo

        val authenticated = createIndividual(listOf(put42), SampleType.SECURITY)
        val forgottenAuth = createIndividual(listOf(get42NotAuth), SampleType.SECURITY)

        val ind = RestIndividualBuilder.merge(authenticated.individual, forgottenAuth.individual)
        assertEquals(HttpVerb.PUT,  ind.seeMainExecutableActions()[0].verb)
        assertEquals(HttpVerb.GET,  ind.seeMainExecutableActions()[1].verb)

        ForgottenAuthenticationApplication.reset()
        val ff = injector.getInstance(AbstractRestFitness::class.java)
        val ei = ff.calculateCoverage(ind)!!

        val r0 = ei.evaluatedMainActions()[0].result as RestCallResult
        val r1 = ei.evaluatedMainActions()[1].result as RestCallResult
        assertEquals(201, r0.getStatusCode())
        assertEquals(200, r1.getStatusCode())

        // we couldn't say this is forgotten because GET could be open, so we cannot be sure.
        val faultDetected = RestSecurityOracle.hasForgottenAuthentication(put42.getName(), ei.individual, ei.seeResults())
        assertFalse(faultDetected)

        assertEquals(0, r0.getFaults().size)
        assertEquals(0, r1.getFaults().size)
    }
}