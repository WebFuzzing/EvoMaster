package org.evomaster.e2etests.spring.openapi.v3.swagger

import com.foo.rest.examples.spring.openapi.v3.swagger.PerfectSwaggerController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PerfectSwaggerEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PerfectSwaggerController())
        }
    }

    @Test
    fun test() {
        runTestHandlingFlakyAndCompilation(
            "PerfectSwaggerEM",
            500
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/v1", "GET is working")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/v1", "POST is working")
        }
    }
}
