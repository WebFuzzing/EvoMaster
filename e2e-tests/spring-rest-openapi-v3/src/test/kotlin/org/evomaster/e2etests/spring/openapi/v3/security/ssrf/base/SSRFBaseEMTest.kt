package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.base

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.base.SSRFBaseController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SSRFBaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(SSRFBaseController(), config)
        }
    }

    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "SSRFBaseEMTest",
            50,
        ) { args: MutableList<String> ->

            // If mocking enabled, it'll spin new services each time when there is a valid URL.
            setOption(args, "externalServiceIPSelectionStrategy", "NONE")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")

            setOption(args, "languageModelConnector", "false")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTrue { solution.hasAnySSRFFaults() }

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch", "OK")
        }
    }
}
