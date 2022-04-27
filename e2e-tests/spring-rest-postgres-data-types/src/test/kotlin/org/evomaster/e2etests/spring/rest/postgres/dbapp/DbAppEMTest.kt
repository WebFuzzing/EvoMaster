package org.evomaster.e2etests.spring.rest.postgres.dbapp

import com.foo.spring.rest.postgres.dbapp.DbAppController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class DbAppEMTest : SpringRestPostgresTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initKlass(DbAppController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "DbApp",
                "org.bar.DbAppEM",
                200
        ) { args ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/integerTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/integerTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/arbitraryPrecisionNumbers", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/arbitraryPrecisionNumbers", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/floatingPointTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/floatingPointTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/monetaryTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/monetaryTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/characterTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/characterTypes", null)

//            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/binaryDataTypes", null)
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/binaryDataTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/booleanType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/booleanType", null)

//            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/networkAddressTypes", null)
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/networkAddressTypes", null)

//            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/bitStringTypes", null)
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/bitStringTypes", null)


            //assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/geometricTypes", null)
            //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/geometricTypes", null)

            //assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/serialTypes", null)
            //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/serialTypes", null)

            //assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/dateTimeTypes", null)
            //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/dateTimeTypes", null)

        }
    }
}