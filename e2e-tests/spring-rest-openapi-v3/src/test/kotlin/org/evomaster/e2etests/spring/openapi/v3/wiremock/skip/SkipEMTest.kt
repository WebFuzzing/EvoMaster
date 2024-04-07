package org.evomaster.e2etests.spring.openapi.v3.wiremock.skip

import com.foo.rest.examples.spring.openapi.v3.wiremock.skip.SkipController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SkipEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(SkipController(), config)
        }
    }

    /**
     * This is a basic test. Need to create an EM test, since no way to track the
     * assertion where there should be no success results, only this is added.
     */
    @Test
    fun testSkippedExternalServicesEntry() {
        val args = arrayOf(
            "--createTests", "false",
            "--seed", "42",
            "--sutControllerPort", "" + controllerPort,
            "--maxActionEvaluations", "1",
            "--stoppingCriterion", "FITNESS_EVALUATIONS",
            "--executiveSummary", "false",
            "--expectationsActive", "true",
            "--outputFormat", "JAVA_JUNIT_5",
            "--outputFolder", "target/em-tests/SkipExternalServiceEM",
            "--externalServiceIPSelectionStrategy", "USER",
            "--externalServiceIP", "127.0.0.4"
        )

        val injector = init(args.toList())

        val externalServiceHandler = injector.getInstance(
            HttpWsExternalServiceHandler::class.java
        )

        assertEquals(1, externalServiceHandler.getSkippedExternalServices().size)

    }
}
