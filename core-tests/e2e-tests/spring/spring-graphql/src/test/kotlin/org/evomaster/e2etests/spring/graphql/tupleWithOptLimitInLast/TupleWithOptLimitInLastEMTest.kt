package org.evomaster.e2etests.spring.graphql.tupleWithOptLimitInLast


import com.foo.graphql.tupleWithOptLimitInLast.TupleWithOptLimitInLastController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TupleWithOptLimitInLastEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TupleWithOptLimitInLastController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_tupleWithOptLimitInLastEM",
                "org.foo.graphql.tupleWithOptLimitInLastEM",
                50
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())
            args.add("--treeDepth")
            args.add("2")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertNoneWithErrors(solution)
        }
    }
}