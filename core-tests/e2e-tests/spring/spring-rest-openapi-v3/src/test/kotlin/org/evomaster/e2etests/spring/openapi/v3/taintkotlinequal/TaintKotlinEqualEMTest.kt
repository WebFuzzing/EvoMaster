package org.evomaster.e2etests.spring.openapi.v3.taintkotlinequal

import com.foo.rest.examples.spring.openapi.v3.taintkotlinequal.TaintKotlinEqualController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TaintKotlinEqualEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TaintKotlinEqualController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "TaintKotlinEqualEM",
                "org.foo.TaintKotlinEqualEM",
                1_000
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintkotlinequal/{a}", "OK")
        }
    }
}