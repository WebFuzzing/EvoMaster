package org.evomaster.e2etests.spring.rest.postgres.compositepk

import com.foo.spring.rest.postgres.compositepk.PostgresCompositePKController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PostgresCompositePKEMTest : SpringRestPostgresTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initKlass(PostgresCompositePKController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "PostgresCompositePKEM",
                "com.foo.spring.rest.postgres.compositepk.PostgresCompositePKEM",
                1_000
        ) { args ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/compositepk/testPK", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/compositepk/testPK", null)
        }
    }
}
