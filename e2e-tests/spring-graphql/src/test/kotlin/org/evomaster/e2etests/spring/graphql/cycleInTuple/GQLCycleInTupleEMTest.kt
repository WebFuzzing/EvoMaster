package org.evomaster.e2etests.spring.graphql.cycleInTuple


import com.foo.graphql.cycleInTuple.CycleInTupleController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLCycleInTupleEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(CycleInTupleController())
        }
    }



    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_CycleInTupleEM",
                "org.foo.graphql.CycleInTupleEM",
                300
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertNoneWithErrors(solution)
        }
    }
}