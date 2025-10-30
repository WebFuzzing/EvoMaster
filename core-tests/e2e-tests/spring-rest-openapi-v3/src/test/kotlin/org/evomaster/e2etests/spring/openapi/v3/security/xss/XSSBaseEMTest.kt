package org.evomaster.e2etests.spring.openapi.v3.security.xss

import com.foo.rest.examples.spring.openapi.v3.security.xss.XSSBaseController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSBaseEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSBaseController(), config)
        }
    }

    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSBaseEMTest",
            50,
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTrue { solution.hasXssFaults() }

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/great", "OK")
        }
    }
}
