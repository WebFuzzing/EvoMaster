package org.evomaster.core.problem.rest.securityrestoracle

import bar.examples.it.spring.securityforbiddenoperation.SecurityForbiddenOperationApplication
import bar.examples.it.spring.securityforbiddenoperation.SecurityForbiddenOperationController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.JdkIssue
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.builder.CreateResourceUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.oracle.RestSecurityOracle
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
            JdkIssue.fixPatchMethod()

            initClass(SecurityForbiddenOperationController())
        }
    }

    @BeforeEach
    fun initializeTest(){
        SecurityForbiddenOperationApplication.reset()
    }

    @Test
    fun testDeletePatch(){

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

        val faultDetected = RestSecurityOracle.hasForbiddenOperation(HttpVerb.DELETE,ind.individual, ind.seeResults())
        assertTrue(faultDetected)
    }


    @Test
    fun testDeletePut(){

        val pirTest = getPirToRest()

        val a = pirTest.fromVerbPath("POST", "/api/resources")!!
        val b = pirTest.fromVerbPath("DELETE", "/api/resources/1234")!!
        CreateResourceUtils.linkDynamicCreateResource(a,b)//FIXME should be in PirToRest
        val c = pirTest.fromVerbPath("PUT", "/api/resources/333")!!
        CreateResourceUtils.linkDynamicCreateResource(a,c)//FIXME should be in PirToRest

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)

        a.auth = foo
        b.auth = bar
        c.auth = bar

        SecurityForbiddenOperationApplication.disabledCheckPut = true

        val ind = createIndividual(listOf(a,b,c), SampleType.SECURITY)
        assertEquals(201, (ind.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (ind.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())
        assertEquals(204, (ind.evaluatedMainActions()[2].result as RestCallResult).getStatusCode())

        val faultDetected = RestSecurityOracle.hasForbiddenOperation(HttpVerb.DELETE,ind.individual, ind.seeResults())
        assertTrue(faultDetected)
    }


    @Test
    fun testReuseTestFaultyPut(){

        val pirTest = getPirToRest()
        val archive = getArchive()
        val security = getSecurityRest()
        val config = getEMConfig()

        config.security = true
        config.schemaOracles = false

        val id = 42
        val a = pirTest.fromVerbPath("PUT", "/api/resources/$id")!!
        val b = pirTest.fromVerbPath("DELETE", "/api/resources/$id")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)
        a.auth = foo
        b.auth = bar

        SecurityForbiddenOperationApplication.disabledCheckPut = true

        val ind = createIndividual(listOf(a,b), SampleType.SECURITY)
        assertEquals(201, (ind.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (ind.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())

        val added = archive.addIfNeeded(ind)
        assertTrue(added)

        val solution = security.applySecurityPhase()

        val target = solution.individuals.find { it.hasAnyPotentialFault() }!!

        val faults = DetectedFaultUtils.getDetectedFaultCategories(target)
        assertEquals(1, faults.size)
        assertEquals(FaultCategory.SECURITY_FORBIDDEN_DELETE, faults.first())

        assertEquals(3, target.individual.size())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[0].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[1].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[2].resolvedPath())
    }


    @Test
    fun testReuseTestFaultyDelete(){

        val pirTest = getPirToRest()
        val archive = getArchive()
        val security = getSecurityRest()
        val config = getEMConfig()

        config.security = true
        config.schemaOracles = false

        val id = 42
        val a = pirTest.fromVerbPath("PUT", "/api/resources/$id")!!
        val b = pirTest.fromVerbPath("PUT", "/api/resources/$id")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)
        a.auth = foo
        b.auth = bar

        SecurityForbiddenOperationApplication.disabledCheckDelete = true

        val ind = createIndividual(listOf(a,b), SampleType.SECURITY)
        assertEquals(201, (ind.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (ind.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())
        val added = archive.addIfNeeded(ind)
        assertTrue(added)

        //we need at least a DELETE or PATCH in the archive, as cannot
        //reuse the same test for a PUT
        val otherID = id + 123
        val c = pirTest.fromVerbPath("PUT", "/api/resources/$otherID")!!
        val d = pirTest.fromVerbPath("DELETE", "/api/resources/$otherID")!!
        c.auth = foo
        d.auth = foo
        val other = createIndividual(listOf(c,d), SampleType.RANDOM)
        assertEquals(201, (other.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(204, (other.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())
        val otherAdded = archive.addIfNeeded(other)
        assertTrue(otherAdded)


        val solution = security.applySecurityPhase()

        val target = solution.individuals.find { it.hasAnyPotentialFault() }!!

        val faults = DetectedFaultUtils.getDetectedFaultCategories(target)
        assertEquals(1, faults.size)
        assertEquals(FaultCategory.SECURITY_FORBIDDEN_PUT, faults.first())

        assertEquals(3, target.individual.size())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[0].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[1].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[2].resolvedPath())
    }


    @Test
    fun testReuseTestFaultyPutAndDelete(){

        val pirTest = getPirToRest()
        val archive = getArchive()
        val security = getSecurityRest()
        val config = getEMConfig()

        config.security = true
        config.schemaOracles = false

        val id = 42
        val a = pirTest.fromVerbPath("PUT", "/api/resources/$id")!!
        val b = pirTest.fromVerbPath("PATCH", "/api/resources/$id")!!

        val auth = controller.getInfoForAuthentication()
        val foo = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "FOO" }!!)
        val bar = HttpWsAuthenticationInfo.fromDto(auth.find { it.name == "BAR" }!!)
        a.auth = foo
        b.auth = bar

        SecurityForbiddenOperationApplication.disabledCheckDelete = true
        SecurityForbiddenOperationApplication.disabledCheckPut = true

        val ind = createIndividual(listOf(a,b), SampleType.SECURITY)
        assertEquals(201, (ind.evaluatedMainActions()[0].result as RestCallResult).getStatusCode())
        assertEquals(403, (ind.evaluatedMainActions()[1].result as RestCallResult).getStatusCode())

        val added = archive.addIfNeeded(ind)
        assertTrue(added)

        val solution = security.applySecurityPhase()

        val target = solution.individuals.find { it.hasAnyPotentialFault() }!!

        val faults = DetectedFaultUtils.getDetectedFaultCategories(target)
        assertEquals(1, faults.size)
        assertEquals(FaultCategory.SECURITY_FORBIDDEN_PATCH, faults.first())

        assertEquals(3, target.individual.size())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[0].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[1].resolvedPath())
        assertEquals("/api/resources/$id", target.individual.seeMainExecutableActions()[2].resolvedPath())
    }

}