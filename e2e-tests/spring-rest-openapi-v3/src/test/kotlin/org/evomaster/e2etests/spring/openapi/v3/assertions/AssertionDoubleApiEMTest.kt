package org.evomaster.e2etests.spring.openapi.v3.assertions

import com.foo.rest.examples.spring.openapi.v3.double.DoubleController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */
class AssertionDoubleApiEMTest : SpringTestBase() {

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

        defaultSeed = 43

        runTestHandlingFlakyAndCompilation(
                "AssertionEM",
                "org.foo.AssertionEM_Double",
                1_000
        ) { args: MutableList<String> ->

            // args.add("--outputFormat")
            // args.add("JAVA_JUNIT_5")
            args.add("--enableBasicAssertions")
            args.add("false")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/double/{x}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/double/{x}", "OK")
        }
    }
}