package org.evomaster.e2etests.spring.openapi.v3.jackson.maplistint

import com.foo.rest.examples.spring.openapi.v3.jackson.maplistint.MapListIntJacksonController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MapListIntJacksonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MapListIntJacksonController())
        }
    }

    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "MapListIntJacksonEM",
            2000
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/maplistint", "Working")
        }
    }
}
