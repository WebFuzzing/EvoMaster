package org.evomaster.e2etests.spring.openapi.v3.security.xss.stored

import com.foo.rest.examples.spring.openapi.v3.security.xss.stored.XSSStoredController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSStoredEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSStoredController(), config)
        }
    }

    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSStoredEMTest",
            50,
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.isNotEmpty())
            Assertions.assertTrue { solution.hasXssFaults() }

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/great", "OK")
        }
    }
}
