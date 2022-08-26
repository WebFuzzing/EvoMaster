package org.evomaster.e2etests.spring.openapi.v3.jackson

import com.foo.rest.examples.spring.openapi.v3.jackson.JacksonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled

class JacksonTypeEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JacksonController())
        }
    }

    @Disabled("Give up")
    fun testTypeReadValue() {
        runTestHandlingFlakyAndCompilation(
            "JacksonTypeEM",
            "org.foo.JacksonTypeEM",
            500
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/type", "Working")
        }
    }
}