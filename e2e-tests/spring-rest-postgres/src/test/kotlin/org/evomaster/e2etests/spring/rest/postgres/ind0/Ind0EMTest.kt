package org.evomaster.e2etests.spring.rest.postgres.ind0

import com.foo.spring.rest.postgres.ind0.Ind0Controller
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class Ind0EMTest : SpringRestPostgresTestBase(){

    companion object {
            @BeforeAll @JvmStatic
            fun initClass() {
                SpringRestPostgresTestBase.initKlass(Ind0Controller())
            }
        }

        @Test
        fun testRunEM() {

            runTestHandlingFlakyAndCompilation(
                    "Ind0EM",
                    "org.bar.Ind0EM",
                    100
            ) { args ->

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertHasAtLeastOne(solution, HttpVerb.GET, 400)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            }
    }
}