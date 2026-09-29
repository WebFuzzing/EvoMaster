package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.invalidlocationfp

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidlocationfp.HttpInvalidLocationFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpOracleInvalidLocationFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidLocationFPController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpOracleInvalidLocationFPEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/invalid-location-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/invalid-location-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/api/invalid-location-fp/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 204, "/api/invalid-location-fp/{id}", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_INVALID_LOCATION in faults )
        }
    }
}
