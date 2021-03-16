package org.evomaster.e2etests.spring.graphql.input

import com.foo.graphql.base.BaseController
import com.foo.graphql.input.BaseInputController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BaseInputEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseInputController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_BaseInputEM",
                "org.foo.graphql.BaseInputEM",
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