package org.evomaster.e2etests.spring.openapi.v3.logintoken

import com.foo.rest.examples.spring.openapi.v3.logintoken.LoginTokenController
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class LoginTokenEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(LoginTokenController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "LoginTokenEM",
                "org.foo.LoginTokenEM",
                20
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "OK")
        }
    }
}