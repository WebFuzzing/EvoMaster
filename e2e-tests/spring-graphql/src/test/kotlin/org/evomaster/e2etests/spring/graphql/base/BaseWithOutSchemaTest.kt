package org.evomaster.e2etests.spring.graphql.base


import com.foo.graphql.base.BaseWithOutschemaController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BaseWithOutSchemaTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseWithOutschemaController())
        }
    }



    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_BaseWithOutSchemaEM",
            "org.foo.graphql.BaseWithOutSchemaEM",
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