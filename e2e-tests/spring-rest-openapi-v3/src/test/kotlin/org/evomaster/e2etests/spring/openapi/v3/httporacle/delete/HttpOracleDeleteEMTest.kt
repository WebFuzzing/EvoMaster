package org.evomaster.e2etests.spring.openapi.v3.httporacle.delete

import com.foo.rest.examples.spring.openapi.v3.httporacle.delete.HttpOracleDeleteController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpOracleDeleteEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpOracleDeleteController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpOracleDeleteEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 204, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/api/resources/{id}", null)


            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertEquals(1, faults.size)
            assertEquals(FaultCategory.HTTP_NONWORKING_DELETE, faults.first())
        }
    }
}