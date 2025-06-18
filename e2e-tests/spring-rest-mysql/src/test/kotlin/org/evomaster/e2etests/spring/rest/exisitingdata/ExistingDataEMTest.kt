package org.evomaster.e2etests.spring.rest.exisitingdata

import com.foo.spring.rest.mysql.exisitingdata.ExistingDataController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExistingDataEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initClass(ExistingDataController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "ExistingDataEM",
            "org.bar.mysql.ExistingDataEM",
            100
        ) { args ->
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
        }
    }
}