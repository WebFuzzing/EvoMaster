package org.evomaster.e2etests.spring.graphql.db.tree

import com.foo.graphql.db.tree.DbTreeController
import com.foo.graphql.db.tree.QueryResolver
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DbTreeEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbTreeController())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testEM(heuristicsForSQLAdvanced: Boolean){

        defaultSeed = 5

        runTestHandlingFlakyAndCompilation(
            "GQL_DbTreeEM",
            "org.foo.graphql.DbTreeEM" +
                    if (heuristicsForSQLAdvanced) "Complete" else "Partial",
            1000
        ) { args: MutableList<String> ->

            setOption(args, "problemType", EMConfig.ProblemType.GRAPHQL.toString())
            setOption(args, "heuristicsForSQL","true")
            setOption(args,"generateSqlDataWithSearch","true" )
            setOption(args,"heuristicsForSQLAdvanced", if (heuristicsForSQLAdvanced) "true" else "false")

            //issues with CI... trying to debug
            setOption(args,"useTimeInFeedbackSampling","false")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, "dbTreeByParentId", GQMethodType.QUERY, 200, QueryResolver.NOT_FOUND)
            assertHasAtLeastOne(solution, "dbTreeByParentId", GQMethodType.QUERY, 200, QueryResolver.NO_PARENT)
            assertHasAtLeastOne(solution, "dbTreeByParentId", GQMethodType.QUERY, 200, QueryResolver.WITH_PARENT)
            /*
                with enabled taint analysis, there might exist errors while executing query
             */
            //assertNoneWithErrors(solution)
        }

    }
}
