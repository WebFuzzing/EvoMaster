package org.evomaster.e2etests.spring.openapi.v3.jackson.liststring

import com.foo.rest.examples.spring.openapi.v3.jackson.liststring.ListStringJacksonController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ListStringJacksonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ListStringJacksonController())
        }
    }

    @Disabled("Need to be able to handle contains() in tainted arrays")
    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "ListStringJacksonEMTest",
            2_000
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/list/string", "Working")
        }
    }
}
