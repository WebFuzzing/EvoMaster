package org.evomaster.e2etests.spring.openapi.v3.security.xss.reflected

import com.foo.rest.examples.spring.openapi.v3.security.xss.reflected.XSSReflectedController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class XSSReflectedEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = false
            initClass(XSSReflectedController(), config)
        }
    }

    @Test
    fun testSSRFEM() {
        runTestHandlingFlakyAndCompilation(
            "XSSReflectedEMTest",
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
