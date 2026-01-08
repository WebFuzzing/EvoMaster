package org.evomaster.core.problem.rest.securityrestoracle

import bar.examples.it.spring.simplesecuritydeleteput.SimpleSecurityDeletePutController
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.oracle.RestSecurityOracle
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled

class SimpleSecurityDeletePutControllerTest : IntegrationTestRestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SimpleSecurityDeletePutController())
        }
    }

    @Disabled // TODO put back once fixed
    @Test
    fun testHandleForbiddenDelete() {

        /*
            - create test cases manually with Pir
            - they ll not be SampleType.SECURITY, so oracle not computed
            - call RestSecurityOracle directly on such created individual
            - verify properties
         */
        // TODO initialize parameters and authentication in those objects.

        val pirTest = getPirToRest()

        val firstAction = pirTest.fromVerbPath("PUT", "/api/endpoint/endpoint2")!!

        val secondAction = pirTest.fromVerbPath("PUT", "/api/endpoint/endpoint2")!!

        val action1Ind1 = pirTest.fromVerbPath("GET", "/api/endpoint2/1000")!!
        val action2Ind1 = pirTest.fromVerbPath("POST", "/api/endpoint1")!!
        val action3Ind1 = pirTest.fromVerbPath("PUT", "/api/endpoint3/2000")!!
        val action4Ind1 = pirTest.fromVerbPath("DELETE", "/api/endpoint4/1005")!!
        val action5Ind1 = pirTest.fromVerbPath("GET", "/api/endpoint5/setStatus/402")!!

        val g1 = StringGene("sampleString")
        g1.markAllAsInitialized()

        firstAction.addParam(QueryParam("param1", g1))


        val sampleInd = createIndividual(listOf(action1Ind1, action2Ind1, action3Ind1, action4Ind1, action5Ind1), SampleType.SECURITY)

        val testCovered = RestSecurityOracle.hasForbiddenOperation(HttpVerb.DELETE,sampleInd.individual, sampleInd.seeResults() )

        assertTrue(testCovered)
    }
}