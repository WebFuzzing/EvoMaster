package org.evomaster.e2etests.spring.openapi.v3.security.ssrf.base

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.base.SSRFBaseController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SSRFBaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SSRFBaseController())
        }
    }

    @Disabled("WIP")
    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "SSRFEMTest",
            300,
        ) { args: MutableList<String> ->

            setOption(args, "externalServiceIPSelectionStrategy", "USER")
            setOption(args, "externalServiceIP", "127.0.0.4")
            setOption(args, "instrumentMR_NET", "true")

            setOption(args, "security", "true")
            setOption(args, "ssrf", "true")
            setOption(args, "vulnerableInputClassificationStrategy", "LLM")

            setOption(args, "languageModelConnector", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/fetch/data", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/fetch/image", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/data", "Unable to fetch sensor data.")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/image", "Unable to fetch remote image.")
        }
    }
}
