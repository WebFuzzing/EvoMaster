package org.evomaster.e2etests.spring.openapi.v3.trace

import com.foo.rest.examples.spring.openapi.v3.trace.TraceController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class TraceEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TraceController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "TraceEM",
                "org.foo.TraceEM",
                20
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.TRACE, 405, "/api/trace", null)
        }
    }
}