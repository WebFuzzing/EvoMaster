package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.nonidempotentputfp

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.nonidempotentputfp.HttpNonIdempotentPutFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpNonIdempotentPutFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpNonIdempotentPutFPController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpNonIdempotentPutFPEM",
                500
        ) { args: MutableList<String> ->

            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/accounts", null)
            assertHasAtLeastOne(solution, HttpVerb.GET,  404, "/api/accounts/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET,  200, "/api/accounts/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT,  404, "/api/accounts/{id}/deposit", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT,  202, "/api/accounts/{id}/deposit", null)


            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_NON_IDEMPOTENT_PUT in faults )
        }
    }
}