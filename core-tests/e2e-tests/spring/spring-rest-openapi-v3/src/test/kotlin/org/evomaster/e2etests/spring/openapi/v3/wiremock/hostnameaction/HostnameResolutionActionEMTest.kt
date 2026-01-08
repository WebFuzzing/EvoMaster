package org.evomaster.e2etests.spring.openapi.v3.wiremock.hostnameaction

import com.foo.rest.examples.spring.openapi.v3.wiremock.hostnameaction.HostnameResolutionActionController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.service.fitness.ResourceRestFitness
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
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

                    FIXME but this can be solved!!! see comments in AbstractRestFitness
                 */
                assertNone(solution, HttpVerb.GET, 400)
                assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/resolve",null)
            },
            3
        )
    }


    @Test
    fun manualTest() {
        val args = arrayOf(
            "--createTests", "false",
            "--seed", "42",
            "--sutControllerPort", "" + controllerPort,
            "--maxEvaluations", "1",
            "--stoppingCriterion", "ACTION_EVALUATIONS",
            "--executiveSummary", "false",
            "--expectationsActive", "true",
            "--outputFormat", "JAVA_JUNIT_5",
            "--outputFolder", "target/em-tests/HostnameResolutionActionTest",
            "--externalServiceIPSelectionStrategy", "USER",
            "--externalServiceIP", "127.0.0.30"
        )
        val injector = init(args.toList())

        val externalServiceHandler = injector.getInstance(HttpWsExternalServiceHandler::class.java)

        val restResourceFitness = injector.getInstance(ResourceRestFitness::class.java)
        val resourceSampler = injector.getInstance(ResourceSampler::class.java)
        val restIndividual = resourceSampler.sample(false)

        assertEquals(0, externalServiceHandler.getLocalDomainNameMapping().size)

        restResourceFitness.calculateCoverage(restIndividual, setOf())

        assertTrue(externalServiceHandler.getLocalDomainNameMapping().containsKey("imaginary-second.local"))
        assertEquals(1, externalServiceHandler.getLocalDomainNameMapping().size)
    }


}
