package org.evomaster.e2etests.spring.openapi.v3.algorithms

import com.foo.rest.examples.spring.openapi.v3.algorithms.AlgorithmsController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class AlgorithmsEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AlgorithmsController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testRunEM_Algorithms_WB(algorithm: EMConfig.Algorithm) {

        runTestHandlingFlakyAndCompilation(
                "AlgorithmsEM_$algorithm",
                200
        ) { args: MutableList<String> ->

            setOption(args, "algorithm", algorithm.name)

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/algorithms/int/{x}", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/algorithms/int/{x}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/algorithms/double/{x}", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/algorithms/double/{x}", null)
        }
    }
}