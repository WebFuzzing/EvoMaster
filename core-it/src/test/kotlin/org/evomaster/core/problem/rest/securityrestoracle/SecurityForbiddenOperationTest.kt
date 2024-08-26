package org.evomaster.core.problem.rest.securityrestoracle

import bar.examples.it.spring.securityforbiddenoperation.SecurityForbiddenOperationApplication
import bar.examples.it.spring.securityforbiddenoperation.SecurityForbiddenOperationController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.RestCallResult
import org.evomaster.core.problem.rest.RestSecurityOracle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecurityForbiddenOperationTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SecurityForbiddenOperationController())
        }
    }

    @BeforeEach
    fun initializeTest(){

    }

    @Test
    fun testDelete(){

        val pirTest = getPirToRest()

        val id = 42

        val a = pirTest.fromVerbPath("PUT", "/api/resources/$id")!!
        val b = pirTest.fromVerbPath("DELETE", "/api/resources/$id")!!
        val c = pirTest.fromVerbPath("PATCH", "/api/resources/$id")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)

        a.auth = foo
        b.auth = bar
        c.auth = bar

        SecurityForbiddenOperationApplication.disabledCheckPatch = true

        val ind = createIndividual(listOf(a,b,c), SampleType.SECURITY)
        assertEquals(201, (ind.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (ind.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())
        assertEquals(204, (ind.evaluatedMainActions()[2].result as RestCallResult).getStatusCode())

        val faultDetected = RestSecurityOracle.hasForbiddenDelete(ind.individual, ind.seeResults())
        assertTrue(faultDetected)
    }
}