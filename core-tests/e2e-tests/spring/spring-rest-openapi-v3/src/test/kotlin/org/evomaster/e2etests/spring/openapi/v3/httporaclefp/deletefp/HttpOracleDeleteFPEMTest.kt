package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.deletefp

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.deletefp.HttpOracleDeleteFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpOracleDeleteFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpOracleDeleteFPController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpOracleDeleteFPEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/api/resources/{id}", null)

            //202 does not mean it is completed... so should not say found fault
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 202, "/api/resources/{id}", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_NONWORKING_DELETE in faults )
        }
    }
}
