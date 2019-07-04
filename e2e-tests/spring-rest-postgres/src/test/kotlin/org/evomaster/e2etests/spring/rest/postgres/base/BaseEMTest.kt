package org.evomaster.e2etests.spring.rest.postgres.base


import com.foo.spring.rest.postgres.base.BaseController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class BasicEMTest : SpringRestPostgresTestBase(){

    companion object {
        @BeforeAll @JvmStatic
        fun initClass() {
            SpringRestPostgresTestBase.initKlass(BaseController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "BaseEM",
                "org.bar.BaseEM",
                100
        ) { args ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
        }
    }
}