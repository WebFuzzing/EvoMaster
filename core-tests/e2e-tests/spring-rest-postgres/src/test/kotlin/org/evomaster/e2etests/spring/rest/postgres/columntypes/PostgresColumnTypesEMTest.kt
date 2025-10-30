package org.evomaster.e2etests.spring.rest.postgres.columntypes

import com.foo.spring.rest.postgres.columntypes.PostgresColumnTypesController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by jgaleotti on 27-apr-22.
 */
class PostgresColumnTypesEMTest : SpringRestPostgresTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initKlass(PostgresColumnTypesController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "PostgresColumnTypesEM",
                "com.foo.spring.rest.postgres.columntypes.PostgresColumnTypesEM",
                600
        ) { args ->
            args.add("--enableWeightBasedMutationRateSelectionForGene")
            args.add("false")

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

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/uuidType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/uuidType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/booleanType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/booleanType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/xmlType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/xmlType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/serialTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/serialTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/builtInRangeTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/builtInRangeTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/jsonTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/jsonTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/binaryDataTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/binaryDataTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/networkAddressTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/networkAddressTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/bitStringTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/bitStringTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/geometricTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/geometricTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/dateTimeTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/dateTimeTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/textSearchTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/textSearchTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/enumType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/enumType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/compositeType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/compositeType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/nestedCompositeType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/nestedCompositeType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/arrayTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/arrayTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/pglsnType", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/pglsnType", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/builtInMultiRangeTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/builtInMultiRangeTypes", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/postgres/objectIdentifierTypes", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/postgres/objectIdentifierTypes", null)

        }
    }
}