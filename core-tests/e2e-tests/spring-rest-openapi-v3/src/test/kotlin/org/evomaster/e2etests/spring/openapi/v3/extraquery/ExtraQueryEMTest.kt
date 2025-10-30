package org.evomaster.e2etests.spring.openapi.v3.extraquery

import com.foo.rest.examples.spring.openapi.v3.extraquery.ExtraQueryController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class ExtraQueryEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExtraQueryController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "ExtraQueryEM",
            "org.foo.ExtraQueryEM",
            1000
        ) { args: MutableList<String> ->

            args.add("--extraQueryParam")
            args.add("true")
            args.add("--searchPercentageExtraHandling")
            args.add("0.7")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 405) //this happens if _method is not handled
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/servlet", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/proxyprint", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/languagetool", "OK")
        }
    }

}