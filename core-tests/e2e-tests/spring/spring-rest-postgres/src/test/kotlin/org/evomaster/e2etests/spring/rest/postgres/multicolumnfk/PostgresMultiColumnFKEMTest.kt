package org.evomaster.e2etests.spring.rest.postgres.multicolumnfk

import com.foo.spring.rest.postgres.multicolumnfk.PostgresMultiColumnFKController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PostgresMultiColumnFKEMTest : SpringRestPostgresTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initKlass(PostgresMultiColumnFKController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "PostgresMultiColumnFKEM",
                "com.foo.spring.rest.postgres.multicolumnfk.PostgresMultiColumnFKEM",
                1_000
        ) { args ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/multicolumnfk/testFK", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/multicolumnfk/testFK", null)
        }
    }
}
