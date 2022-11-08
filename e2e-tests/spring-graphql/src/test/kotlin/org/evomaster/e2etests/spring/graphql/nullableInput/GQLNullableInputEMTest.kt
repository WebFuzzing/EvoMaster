package org.evomaster.e2etests.spring.graphql.nullableInput

import com.foo.graphql.nullableInput.NullableInputController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLNullableInputEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NullableInputController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_NullableInputEM",
                "org.foo.graphql.NullableInputEM",
                20
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertValueInDataAtLeastOnce(solution, "flowersNullInNullOut")
            assertValueInDataAtLeastOnce(solution, "flowersNullIn")
            assertValueInDataAtLeastOnce(solution, "flowersNullOut")
            assertValueInDataAtLeastOnce(solution, "flowersNotNullInOut")
            assertValueInDataAtLeastOnce(solution, "flowersScalarNullable")
            assertValueInDataAtLeastOnce(solution, "flowersScalarNotNullable")
            assertNoneWithErrors(solution)
        }
    }
}