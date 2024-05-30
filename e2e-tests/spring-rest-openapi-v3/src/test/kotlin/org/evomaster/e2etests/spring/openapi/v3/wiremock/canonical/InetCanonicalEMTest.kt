package org.evomaster.e2etests.spring.openapi.v3.wiremock.canonical

import com.foo.rest.examples.spring.openapi.v3.wiremock.canonical.InetCanonicalController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Disabled until the implementation is completed")
class InetCanonicalEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(InetCanonicalController(), config)
        }
    }

    @Test
    fun test() {
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
            "--externalServiceIP", "127.0.0.46"
        )

        val injector = init(args.toList())

        val externalServiceHandler = injector.getInstance(
            HttpWsExternalServiceHandler::class.java
        )

        assertEquals(1, externalServiceHandler.getSkippedExternalServices().size)

    }
}
