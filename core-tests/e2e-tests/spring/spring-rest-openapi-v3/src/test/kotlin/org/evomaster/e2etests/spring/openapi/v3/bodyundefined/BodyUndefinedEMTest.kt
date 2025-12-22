package org.evomaster.e2etests.spring.openapi.v3.bodyundefined

import com.foo.rest.examples.spring.openapi.v3.bodyundefined.BodyUndefinedController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BodyUndefinedEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BodyUndefinedController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "BodyUndefinedEM",
                50
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 400, "/api/bodyundefined", "FAIL")
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/api/bodyundefined", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bodyundefined", "FAIL")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bodyundefined", "OK")
        }
    }
}