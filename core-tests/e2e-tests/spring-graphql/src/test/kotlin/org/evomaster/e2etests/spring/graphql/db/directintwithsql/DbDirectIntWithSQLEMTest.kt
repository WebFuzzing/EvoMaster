package org.evomaster.e2etests.spring.graphql.db.directintwithsql

import com.foo.graphql.db.directintwithsql.DbDirectIntWithSQLController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DbDirectIntWithSQLEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbDirectIntWithSQLController())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [ "false", "true"])
    fun testRunEM(heuristicsForSQLAdvanced: Boolean) {
        runTestHandlingFlakyAndCompilation(
            "GQL_DbDirectWithSqlEM",
            "org.foo.graphql.DbDirectWithSqlEM" + if (heuristicsForSQLAdvanced) "Complete" else "Partial",
            2000
        ) { args: MutableList<String> ->

            setOption(args, "problemType",EMConfig.ProblemType.GRAPHQL.toString())

            setOption(args, "heuristicsForSQL", "true")
            setOption(args, "generateSqlDataWithSearch", "true")
            setOption(args, "heuristicsForSQLAdvanced", if (heuristicsForSQLAdvanced) "true" else "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            //no data
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, "\"get\":[]")
            //at lest 1 entry
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, "\"get\":[{")
            assertInsertionIntoTable(solution, "DB_DIRECT_INT")
            assertNoneWithErrors(solution)
        }
    }
}
