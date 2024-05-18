package org.evomaster.e2etests.spring.openapi.v3.jackson.auth0

import com.foo.rest.examples.spring.openapi.v3.jackson.auth0.AuthZeroJacksonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class AuthZeroJacksonEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AuthZeroJacksonController())
        }
    }

    @Test
    fun testRunEM() {
        // Generated test has response which is accurate, but test fails because the
        // SUT throws error for the case which worked during the search.
        // When the created tests set to false, the test pass.
        // SUT uses HTTPS so the test won't work on macOS.
        runTestHandlingFlakyAndCompilation(
            "GeneratedAuth0JacksonEMTest",
            "org.foo.GeneratedAuth0JacksonEMTest",
            500,
            true,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.34")
                args.add("--instrumentMR_NET")
                args.add("true")

                val solution = initAndRun(args)

                Assertions.assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/auth", "Working")
            }, 3
        )
    }
}
