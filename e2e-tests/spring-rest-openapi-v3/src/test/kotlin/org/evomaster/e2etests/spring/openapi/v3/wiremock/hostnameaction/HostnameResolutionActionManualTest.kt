package org.evomaster.e2etests.spring.openapi.v3.wiremock.hostnameaction

import com.foo.rest.examples.spring.openapi.v3.wiremock.hostnameaction.HostnameResolutionActionController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.service.fitness.ResourceRestFitness
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HostnameResolutionActionManualTest: SpringTestBase() {

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
