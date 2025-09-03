package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.header

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.header.SSRFHeaderController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SSRFHeaderEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(SSRFHeaderController(), config)
        }
    }

    @Test
    fun testSSRFHeader() {
        runTestHandlingFlakyAndCompilation(
            "SSRFHeaderEMTest",
            30,
        ) { args: MutableList<String> ->

            // If mocking enabled, it'll spin new services each time when there is a valid URL.
            setOption(args, "externalServiceIPSelectionStrategy", "NONE")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTrue{ solution.hasAnySSRFFaults() }
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/header", "OK")
        }
    }
}
