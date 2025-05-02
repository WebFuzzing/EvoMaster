package org.evomaster.e2etests.spring.openapi.v3.time


import com.foo.rest.examples.spring.openapi.v3.time.TimeController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TimeEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TimeController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "TimeEM",
                1000
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/time", "A")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/time", "B")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/time", "C")
            assertNone(solution, HttpVerb.GET, 200, "/api/time", "D")
        }
    }
}