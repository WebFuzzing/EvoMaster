package org.evomaster.e2etests.spring.openapi.v3.jackson

import com.foo.rest.examples.spring.openapi.v3.jackson.JacksonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JacksonEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JacksonController())
        }
    }

    @Test
    fun testGenericReadValue() {
        runTestHandlingFlakyAndCompilation(
            "JacksonEM",
            "org.foo.JacksonEM",
            500
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/generic", "Hello World!!!")
        }
    }

    @Test
    fun testTypeReadValue() {
        runTestHandlingFlakyAndCompilation(
            "JacksonEM",
            "org.foo.JacksonEM",
            500
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/type", "Hello World!!!")
        }
    }
}