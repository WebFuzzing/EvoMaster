package org.evomaster.e2etests.spring.openapi.v3.jackson.mapdto

import com.foo.rest.examples.spring.openapi.v3.jackson.mapdto.MapDtoJacksonController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MapDtoJacksonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MapDtoJacksonController())
        }
    }

    @Disabled("We don't handle this case yet... also, how common would it be???")
    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "MapDtoJacksonEM",
            2000
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/mapdto", "Working")
        }
    }
}
