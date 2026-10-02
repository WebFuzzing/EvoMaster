package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.invalidmergepatchfp

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidmergepatchfp.HttpInvalidMergePatchFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpOracleInvalidMergePatchFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidMergePatchFPController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpOracleInvalidMergePatchFPEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "httpOracles", "true")
            setOption(args, "dtoForRequestPayload", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/merge-patch-fp", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/merge-patch-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/merge-patch-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 404, "/api/merge-patch-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 202, "/api/merge-patch-fp/{id}", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_INVALID_MERGE_PATCH in faults )
        }
    }
}
