package org.evomaster.e2etests.spring.openapi.v3.gson.typedto

import com.foo.rest.examples.spring.openapi.v3.gson.typedto.TypeDtoGsonController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TypeDtoGsonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TypeDtoGsonController())
        }
    }

    @Disabled("Need to be able to handle TypeToken")
    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "TypeDtoGsonEMTestGenerated",
            1_000
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/gson/type", "Working")
        }
    }
}
