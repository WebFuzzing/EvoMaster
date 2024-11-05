package org.evomaster.e2etests.spring.openapi.v3.json.jackson.read

import com.foo.rest.examples.spring.openapi.v3.json.jackson.read.JacksonReadValueController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JacksonReadValueEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_NET = true
            initClass(JacksonReadValueController(), config)
        }
    }

    @Disabled("Test fails")
    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JacksonReadValueEMGenerated",
            1500
        ) { args: MutableList<String> ->
            val solution = initAndRun(args)

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/jackson/read/map", "Bingo!")
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/jackson/read/list", "Bingo!")
            assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/jackson/read/dto", "Bingo!")
        }
    }
}
