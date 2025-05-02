package org.evomaster.e2etests.spring.openapi.v3.jackson.mapdouble

import com.foo.rest.examples.spring.openapi.v3.jackson.mapdouble.MapDoubleJacksonController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MapDoubleJacksonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MapDoubleJacksonController())
        }
    }

    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "MapDoubleJacksonEM",
            1000
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/mapdouble", "Working")
        }
    }
}
