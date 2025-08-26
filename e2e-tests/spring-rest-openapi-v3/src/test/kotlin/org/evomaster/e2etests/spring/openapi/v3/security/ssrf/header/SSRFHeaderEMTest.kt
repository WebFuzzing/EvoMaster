package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.header

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.header.SSRFHeaderController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
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

    @Disabled
    @Test
    fun testSSRFHeader() {
        runTestHandlingFlakyAndCompilation(
            "SSRFEMTest",
            100,
        ) { args: MutableList<String> ->

            setOption(args, "externalServiceIPSelectionStrategy", "NONE")
            setOption(args, "externalServiceIP", "127.0.0.6")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/header", "OK")
        }
    }
}
