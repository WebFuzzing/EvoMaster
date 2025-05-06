package org.evomaster.e2etests.spring.openapi.v3.jackson.auth0

import com.foo.rest.examples.spring.openapi.v3.jackson.auth0.AuthZeroJacksonController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AuthZeroJacksonEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(AuthZeroJacksonController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "Auth0JacksonEM",
            "org.foo.Auth0JacksonEMTest",
            250,
            true,
            { args: MutableList<String> ->

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.34")
                args.add("--instrumentMR_NET")
                args.add("true")

                val solution = initAndRun(args)

                //This doesn't work when test are re-run for flakiness, as info is removed
//                val replacements = UnitsInfoRecorder.getMethodReplacementInfo()
//                val network = replacements.filter { it.contains("OkHttpClient3ClassReplacement.newCall") }
//                assertEquals(1, network.size)

                Assertions.assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/jackson/auth", "Working")
            }, 5
        )
    }
}
