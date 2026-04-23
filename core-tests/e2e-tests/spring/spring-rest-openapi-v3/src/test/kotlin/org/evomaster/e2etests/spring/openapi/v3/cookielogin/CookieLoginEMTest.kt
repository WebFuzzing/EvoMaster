package org.evomaster.e2etests.spring.openapi.v3.cookielogin

import com.foo.rest.examples.spring.openapi.v3.cookielogin.CookieLoginController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CookieLoginEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CookieLoginController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "CookieLoginEM",
                "org.foo.CookieLoginEM",
                20
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "this:foo")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "center:bar")

        }
    }
}