package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.base

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.base.SSRFBaseController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
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
            config.instrumentMR_NET = true
            initClass(SSRFBaseController(), config)
        }
    }

    //    @Disabled("WIP")
    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "SSRFBaseEMTest",
            200,
        ) { args: MutableList<String> ->

            setOption(args, "externalServiceIPSelectionStrategy", "USER")
            setOption(args, "externalServiceIP", "127.0.0.50")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "MANUAL")

            setOption(args, "languageModelConnector", "false")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/data", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/image", "OK")

            assertHasAtLeastOne(solution, HttpVerb.POST, 204, "/api/fetch/data", "Unable to fetch sensor data.")
            assertHasAtLeastOne(solution, HttpVerb.POST, 204, "/api/fetch/image", "Unable to fetch remote image.")
        }
    }
}
