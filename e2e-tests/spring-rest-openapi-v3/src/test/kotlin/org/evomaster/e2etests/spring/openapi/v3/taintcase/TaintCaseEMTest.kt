package org.evomaster.e2etests.spring.openapi.v3.taintcase

import com.foo.rest.examples.spring.openapi.v3.taintcase.TaintCaseController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TaintCaseEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TaintCaseController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "TaintCaseEM",
                "org.foo.TaintCaseEM",
                1_000
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintcase/check/{a}/{b}", "OK")
        }
    }
}