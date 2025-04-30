package org.evomaster.e2etests.spring.openapi.v3.queryparamarray

import com.foo.rest.examples.spring.openapi.v3.queryparamarray.QueryParamArrayController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class QueryParamArrayEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(QueryParamArrayController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "QueryParamArrayEMT",
            "org.foo.QueryParamArrayEMT",
            50
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/queryparamarray", "[")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/queryparamarray", null)

            //after refactoring/fixes, sending empty list seems possible now
            //assertNone(solution, HttpVerb.GET, 400)
        }
    }
}