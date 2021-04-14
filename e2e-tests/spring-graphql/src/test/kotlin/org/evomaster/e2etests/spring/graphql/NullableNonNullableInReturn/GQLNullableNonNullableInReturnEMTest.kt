package org.evomaster.e2etests.spring.graphql.NullableNonNullableInReturn

import com.foo.graphql.nullableNonNullableInReturn.NullableNonNullableInReturnController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLNullableNonNullableInReturnEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NullableNonNullableInReturnController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_NullableNonNullableInReturnEM",
                "org.foo.graphql.NullableNonNullableInReturnEM",
                20
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            //deactivated because there is a bug in the application non in the sent request
            // assertHasAtLeastOneResponseWithData(solution)
            //assertNoneWithErrors(solution)
        }
    }
}