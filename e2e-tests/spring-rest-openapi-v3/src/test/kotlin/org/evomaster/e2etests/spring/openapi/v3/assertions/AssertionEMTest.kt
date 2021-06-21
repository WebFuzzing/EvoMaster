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
                1_000
        ) { args: MutableList<String> ->

            var solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/assertions/data", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/data", "{\"a\":42,\"c\":[1000,2000,3000],\"d\":{\"e\":66,\"f\":\"bar\",\"g\":{\"h\":[\"xvalue\",\"yvalue\"]}},\"i\":true,\"l\":false}")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleNumber", "42")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleString", "simple-string")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleText", "simple-text")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/simpleArray", "123")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/arrayObject", "777")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/arrayEmpty", "[]")

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/assertions/objectEmpty", "{}")
        }
    }

}