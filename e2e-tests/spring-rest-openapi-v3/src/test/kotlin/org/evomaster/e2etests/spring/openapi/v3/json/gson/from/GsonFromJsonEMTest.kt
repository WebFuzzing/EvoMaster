package org.evomaster.e2etests.spring.openapi.v3.json.gson.from

import com.foo.rest.examples.spring.openapi.v3.json.gson.from.GsonFromJsonController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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

    @Disabled("Test fails")
    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GsonFromJsonEMGenerated",
            2000
        ) { args: MutableList<String> ->
            val solution = initAndRun(args)

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/gson/from/class", "Bingo!")
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/gson/from/type", "Bingo!")
        }
    }
}
