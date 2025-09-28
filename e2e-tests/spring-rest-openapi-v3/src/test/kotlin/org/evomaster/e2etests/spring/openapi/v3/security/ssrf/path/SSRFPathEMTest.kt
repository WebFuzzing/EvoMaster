package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.path

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.path.SSRFPathController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SSRFPathEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(SSRFPathController(), config)
        }
    }

//    @Disabled
    @Test
    fun testSSRFPathVariable() {
        runTestHandlingFlakyAndCompilation(
            "SSRFPathEMTest",
            200,
        ) { args: MutableList<String> ->

            // If mocking enabled, it'll spin new services each time when there is a valid URL.
            setOption(args, "externalServiceIPSelectionStrategy", "NONE")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
//            assertTrue{ solution.hasSsrfFaults() }
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/path", "OK")
        }
    }
}
