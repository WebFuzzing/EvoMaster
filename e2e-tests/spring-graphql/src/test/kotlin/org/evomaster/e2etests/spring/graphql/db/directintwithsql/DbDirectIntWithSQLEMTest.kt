package org.evomaster.e2etests.spring.graphql.db.directintwithsql

import com.foo.graphql.db.directintwithsql.DbDirectIntWithSQLController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class DbDirectIntWithSQLEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbDirectIntWithSQLController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_DbDirectWithSqlEM",
            "org.foo.graphql.DbDirectWithSqlEM",
            2000
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--heuristicsForSQL")
            args.add("true")
            args.add("--generateSqlDataWithSearch")
            args.add("true")


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