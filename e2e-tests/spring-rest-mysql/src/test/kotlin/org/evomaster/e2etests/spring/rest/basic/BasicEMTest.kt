package org.evomaster.e2etests.spring.rest.basic

import com.foo.spring.rest.mysql.basic.BasicController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BasicEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            RestTestBase.initClass(BasicController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "BasicEM",
            "org.bar.mysql.BasicEM",
            100
        ) { args ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
        }
    }
}