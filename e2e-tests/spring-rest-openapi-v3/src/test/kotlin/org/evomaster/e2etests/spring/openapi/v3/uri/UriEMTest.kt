package org.evomaster.e2etests.spring.openapi.v3.uri

import com.foo.rest.examples.spring.openapi.v3.uri.UriController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class UriEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(UriController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "UriEM",
            "org.foo.UriEM",
            100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/uri/http", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/uri/data", "OK")
        }
    }

}