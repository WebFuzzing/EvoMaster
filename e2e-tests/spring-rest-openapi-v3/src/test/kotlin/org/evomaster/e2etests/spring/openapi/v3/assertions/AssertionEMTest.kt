package org.evomaster.e2etests.spring.openapi.v3.assertions

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class AssertionEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AssertionController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "AssertionEM",
                "org.foo.AssertionEM",
                20
        ) { args: MutableList<String> ->

            var solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/api/assertions/data", "OK")

        }
    }

}