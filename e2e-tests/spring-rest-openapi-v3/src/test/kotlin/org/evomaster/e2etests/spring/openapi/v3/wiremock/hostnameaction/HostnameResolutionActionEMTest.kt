package org.evomaster.e2etests.spring.openapi.v3.wiremock.hostnameaction

import com.foo.rest.examples.spring.openapi.v3.wiremock.hostnameaction.HostnameResolutionActionController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.service.ResourceSampler
import org.evomaster.core.problem.rest.service.RestResourceFitness
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HostnameResolutionActionEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(HostnameResolutionActionController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "HostnameResolutionActionEMTest",
            "org.foo.HostnameResolutionActionEMTest",
            100,
            true,
            { args: MutableList<String> ->

                // Note: WireMock is initiated based on the served requests.
                // This SUT doesn't make any requests, so [TestSuiteWriter] will not add
                // any WM, eventually the generated tests will fail.
                // TODO: This will fail when [createTests] is true regardless of the
                //  environment.

                args.add("--externalServiceIPSelectionStrategy")
                args.add("USER")
                args.add("--externalServiceIP")
                args.add("127.0.0.14")

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resolve", "OK")
                /*
                    Tricky. First time a test is evaluated, this will happen.
                    But, from there on, WM will always be on, so impossible to replicate,
                    eg, in minimizer or generated tests.
                 */
                assertNone(solution, HttpVerb.GET, 400)
                assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resolve",null)
            },
            3
        )
    }



}
