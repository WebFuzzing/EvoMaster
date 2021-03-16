package org.evomaster.e2etests.spring.graphql.mutation

import com.foo.graphql.mutation.BaseMutationController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BaseMutationEMTest: SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseMutationController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_BaseMutationEM",
                "org.foo.graphql.BaseMutationEM",
                20
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
        }
    }
}