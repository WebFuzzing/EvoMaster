package org.evomaster.e2etests.spring.openapi.v3.charescaperegex

import com.foo.rest.examples.spring.openapi.v3.charescaperegex.CharEscapeRegexController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


//TODO need to fix issue with escaping of chars in regex.
//but, before that, should really fix the taint on sampling
@Disabled
class CharEscapeRegexEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CharEscapeRegexController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "CharEscapeRegexEM",
                "org.foo.CharEscapeRegexEM",
                10_000
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/charescaperegex/x", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/charescaperegex/y", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/charescaperegex/z", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/charescaperegex/z", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/charescaperegex/x", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/charescaperegex/y", "OK")
        }
    }
}