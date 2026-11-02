package org.evomaster.e2etests.spring.openapi.v3.httporaclefp.partialupdateputfp

import com.foo.rest.examples.spring.openapi.v3.httporaclefp.partialupdateputfp.HttpPartialUpdatePutFPController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpPartialUpdatePutFPEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpPartialUpdatePutFPController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpPartialUpdatePutFPEM",
                1000
        ) { args: MutableList<String> ->

            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/resources/{id}", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertFalse( DefinedFaultCategory.HTTP_PARTIAL_UPDATE_PUT in faults )
        }
    }
}
