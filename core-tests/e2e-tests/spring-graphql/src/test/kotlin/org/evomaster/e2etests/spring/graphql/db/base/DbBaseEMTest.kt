package org.evomaster.e2etests.spring.graphql.db.base

import com.foo.graphql.db.base.DbBaseController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import kotlin.booleanArrayOf

class DbBaseEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbBaseController())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testRunEM(heuristicsForSQLAdvanced: Boolean) {
        runTestHandlingFlakyAndCompilation(
            "DbBaseEM" +
                    if (heuristicsForSQLAdvanced) "Complete" else "Partial",
            10000
        ) { args: MutableList<String> ->

            /*
                TODO
                Known issue with string escaping in generated tests, eg \u.
                until fixed, this test might fail, and need to change its seed
             */
            defaultSeed = 3

            setOption(args, "problemType", EMConfig.ProblemType.GRAPHQL.toString())
            setOption(args, "heuristicsForSQL", "true")
            setOption(args, "generateSqlDataWithSearch", "false")
            setOption(args, "heuristicsForSQLAdvanced", if (heuristicsForSQLAdvanced) "true" else "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(
                solution,
                "dbBaseByName",
                GQMethodType.QUERY,
                200,
                Arrays.asList("\"id\":\"42\"", "\"name\":\"foo\""),
                false
            )
            // there exists some problems on addDbBase, e.g., 500 MUTATION addDbBase, auth=NoAuth
            // assertNoneWithErrors(solution)
        }
    }
}
