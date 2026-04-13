package org.evomaster.e2etests.spring.openapi.v3.namedexample

import com.foo.rest.examples.spring.openapi.v3.namedexample.NamedExampleController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class NamedExampleEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NamedExampleController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "NamedExampleEM",
            200,
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/object", "OK")

            TODO add checks on named example target
        }
    }

}
