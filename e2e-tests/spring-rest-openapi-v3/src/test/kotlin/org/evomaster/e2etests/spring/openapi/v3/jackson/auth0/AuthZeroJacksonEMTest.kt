package org.evomaster.e2etests.spring.openapi.v3.jackson.auth0

import com.foo.rest.examples.spring.openapi.v3.jackson.auth0.AuthZeroJacksonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
        /*
            test works fine locally in Windows, but having issues in GA
            TODO need to investigate
         */
        runTestHandlingFlakyAndCompilation(
            "Auth0JacksonEM",
            "org.foo.Auth0JacksonEMTest",
            2500, //TODO reduce
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
            }, 15  //TODO reduce
        )
    }
}
