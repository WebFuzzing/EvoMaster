package org.evomaster.e2etests.spring.rest.bb.externalauth

import com.foo.rest.examples.bb.externalauth.ExternalAuthController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBExternalAuthEMTest : SpringTestBase() {

    companion object {
        init {
            EnterpriseTestBase.shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExternalAuthController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "externalauth",
            20,
            3,
            listOf("token1")
        ){ args: MutableList<String> ->

            setOption(args, "configPath", "src/test/resources/config/external_auth.yaml")
            setOption(args, "endpointFocus", "/api/externalauth/check")
            val uri = java.net.URI(baseUrlOfSut)
            val port = uri.port

            setOption(args, "overrideAuthExternalEndpointURL", "localhost:$port")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token1")
            assertNone(solution, HttpVerb.GET, 200, "/api/externalauth/check", "token2")
        }
    }
}