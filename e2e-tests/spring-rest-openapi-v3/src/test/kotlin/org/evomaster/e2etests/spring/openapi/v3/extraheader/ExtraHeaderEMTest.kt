package org.evomaster.e2etests.spring.openapi.v3.extraheader

import com.foo.rest.examples.spring.openapi.v3.extraheader.ExtraHeaderController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class ExtraHeaderEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExtraHeaderController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "ExtraHeaderEM",
            "org.foo.ExtraHeaderEM",
            100
        ) { args: MutableList<String> ->

            //Does not seem needed for this example, as still using TT... does not look like for headers
            // there is a function returning a map of headers...
            //but maybe could be in other frameworks not dealing with JEE servlets
//            args.add("--extraHeader")
//            args.add("true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraheader", "OK")
        }
    }

}