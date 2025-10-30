package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.query

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.query.SSRFQueryController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SSRFQueryEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(SSRFQueryController(), config)
        }
    }

    @Test
    fun testSSRFQuery() {
        runTestHandlingFlakyAndCompilation(
            "SSRFQueryEMTest",
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
            assertTrue(solution.hasSsrfFaults())

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/query", "OK")
        }
    }
}
