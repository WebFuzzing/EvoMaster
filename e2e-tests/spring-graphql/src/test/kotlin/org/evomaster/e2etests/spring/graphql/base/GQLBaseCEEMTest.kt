package org.evomaster.e2etests.spring.graphql.base

import com.foo.graphql.base.BaseController
import com.foo.graphql.base.BaseCustomEndpointController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GQLBaseCEEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BaseCustomEndpointController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQL_BaseCEEM",
                "org.foo.graphql.BaseCEEM",
                20
        ) { args: MutableList<String> ->
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOneResponseWithData(solution)
            assertValueInDataAtLeastOnce(solution, "Foo")
            assertNoneWithErrors(solution)
        }
    }
}