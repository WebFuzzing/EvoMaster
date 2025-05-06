package org.evomaster.e2etests.spring.openapi.v3.headerobject

import com.foo.rest.examples.spring.openapi.v3.headerobject.HeaderObjectController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HeaderObjectEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HeaderObjectController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "HeaderObjectEM",
                "org.foo.HeaderObjectEM",
                100
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/headerobject", "OK")
        }
    }
}