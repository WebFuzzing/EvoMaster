package org.evomaster.e2etests.spring.openapi.v3.authenticatedswaggerendpoint

import com.foo.rest.examples.spring.openapi.v3.authenticatedswaggerendpoint.AuthenticatedSwaggerEndpointController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AuthenticatedSwaggerEndpointEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AuthenticatedSwaggerEndpointController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "AuthenticatedSwaggerEndpointEM",
                "org.foo.AuthenticatedSwaggerEndpointEM",
                20
        ) { args: List<String> ->

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "OK")
        }
    }

    @Test
    fun testRunBlackBoxEM() {
        runTestHandlingFlakyAndCompilation(
            "AuthenticatedSwaggerEndpointEMBlackBox",
            "org.foo.AuthenticatedSwaggerEndpointEMBlackBox",
            20
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--bbExperiments")
            args.add("false")

            args.add("--configPath")
            args.add("src/main/resources/config/authenticatedswagger.toml")

            args.add("--endpointFocus")
            args.add("/api/logintoken/check")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintoken/check", "OK")
        }
    }

}