package org.evomaster.e2etests.spring.openapi.v3.double

import com.foo.rest.examples.spring.openapi.v3.double.DoubleController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class DoubleEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DoubleController())
        }
    }


    @Test
    fun testDeterminism() {
        runAndCheckDeterminism(1000) { args: List<String> -> initAndRun(args) }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "DoubleEM",
                "org.foo.DoubleEM",
                10_000
        ) { args: List<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/double/{x}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/double/{x}", "OK")
        }
    }
}