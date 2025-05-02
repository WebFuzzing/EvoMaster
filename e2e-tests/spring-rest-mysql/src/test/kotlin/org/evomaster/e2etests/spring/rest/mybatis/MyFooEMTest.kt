package org.evomaster.e2etests.spring.rest.mybatis

import com.foo.spring.rest.mysql.mybatis.MyFooController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MyFooEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            RestTestBase.initClass(MyFooController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "MyFooEM",
            "org.bar.mysql.MyFooEM",
            100
        ) { args ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
        }
    }
}