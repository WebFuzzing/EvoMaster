package org.evomaster.e2etests.spring.openapi.v3.externalauth

import com.foo.rest.examples.spring.openapi.v3.externalauth.ExternalAuthController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExternalAuthEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExternalAuthController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "ExternalAuthEM",
                "org.foo.ExternalAuthEM",
                20
        ) { args: List<String> ->
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token1")
            assertNone(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token2")
        }
    }

    @Test
    fun testRunOverrideExternal() {
        runTestHandlingFlakyAndCompilation(
            "ExternalAuthEM2",
            "org.foo.ExternalAuthEM",
            20
        ) { args: List<String> ->
            setOption(args, "overrideAuthExternalEndpointURL", "$baseUrlOfSut/api/externalauth/login2")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token2")
            assertNone(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token1")
        }
    }
}