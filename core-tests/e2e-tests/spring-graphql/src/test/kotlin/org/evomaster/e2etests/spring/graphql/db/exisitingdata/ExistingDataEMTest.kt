package org.evomaster.e2etests.spring.graphql.db.exisitingdata

import com.foo.graphql.db.exisitingdata.ExistingDataController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

class ExistingDataEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExistingDataController())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testEM(heuristicsForSQLAdvanced: Boolean){
        runTestHandlingFlakyAndCompilation(
            "GQL_DbExistingDataEM",
            "org.foo.graphql.DbExistingDataEM" + if (heuristicsForSQLAdvanced) "Complete" else "Partial",
            500
        ) { args: MutableList<String> ->

            setOption(args, "problemType", EMConfig.ProblemType.GRAPHQL.toString())
            setOption(args, "heuristicsForSQL","true")
            setOption(args, "generateSqlDataWithSearch","true")
            setOption(args, "heuristicsForSQLAdvanced", if (heuristicsForSQLAdvanced) "true" else "false")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, "getY", GQMethodType.QUERY, 200, listOf(
                "\"x\":{\"id\":\"42\",\"name\":\"Foo\"}",
                "\"x\":{\"name\":\"Foo\"}",
                "\"x\":{\"id\":\"42\"}"
            ), false)
            assertHasAtLeastOne(solution, "getY", GQMethodType.QUERY, 200, "\"getY\":[]")
            assertNoneWithErrors(solution)
        }

    }
}
