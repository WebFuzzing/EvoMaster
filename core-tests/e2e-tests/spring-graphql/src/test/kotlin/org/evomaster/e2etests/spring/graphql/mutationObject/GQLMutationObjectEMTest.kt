package org.evomaster.e2etests.spring.graphql.mutationObject


import com.foo.graphql.mutationObject.MutationObjectController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLMutationObjectEMTest: SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MutationObjectController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_MutationObjectEM",
                "org.foo.graphql.MutationObjectEM",
                20
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