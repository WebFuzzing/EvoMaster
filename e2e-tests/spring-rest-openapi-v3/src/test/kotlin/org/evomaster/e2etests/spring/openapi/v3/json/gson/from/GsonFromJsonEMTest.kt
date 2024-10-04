package org.evomaster.e2etests.spring.openapi.v3.json.gson.from

import com.foo.rest.examples.spring.openapi.v3.json.gson.from.GsonFromJsonController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled

class GsonFromJsonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(GsonFromJsonController(), config)
        }
    }

    @Disabled
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GsonFromJsonEM",
            "org.foo.GsonFromJsonEM",
            500,
            true,
            { args: MutableList<String> ->
                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/gson/from", "OK")
            },
            3
        )
    }
}
