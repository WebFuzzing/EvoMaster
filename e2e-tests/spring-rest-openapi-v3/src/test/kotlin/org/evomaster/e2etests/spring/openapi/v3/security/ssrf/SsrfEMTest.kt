package org.evomaster.e2etests.spring.openapi.v3.security.ssrf

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.SsrfController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SsrfEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SsrfController())
        }
    }

    @Disabled("WIP")
    @Test
    fun testSsrfEM() {
        runTestHandlingFlakyAndCompilation(
            "SsrfEMTest",
            200,
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

            assertTrue(solution.individuals.isNotEmpty())

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/fetch/data", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/fetch/image", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/data", "Unable to fetch sensor data.")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/image", "Unable to fetch remote image.")
        }
    }
}
