package org.evomaster.e2etests.spring.openapi.v3.security.ssrf

import com.foo.rest.examples.spring.openapi.v3.security.ssrf.SsrfController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SsrfEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SsrfController())
        }
    }

    @Disabled("Work in progress")
    @Test
    fun testSsrfEM() {

        runTestHandlingFlakyAndCompilation(
            "SsrfEM",
            1000
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/data", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/fetch/image", null)
        }
    }
}
