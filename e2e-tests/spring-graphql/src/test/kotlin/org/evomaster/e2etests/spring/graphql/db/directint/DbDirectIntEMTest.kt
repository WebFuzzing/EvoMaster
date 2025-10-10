package org.evomaster.e2etests.spring.graphql.db.directint

import com.foo.graphql.db.directint.DbDirectIntController
import graphql.com.google.common.base.Predicate
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

class DbDirectIntEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbDirectIntController())
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testAvg(heuristicsForSQLAdvanced: Boolean){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE, heuristicsForSQLAdvanced)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testAvg_SAME_N(heuristicsForSQLAdvanced: Boolean){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS, heuristicsForSQLAdvanced)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testBest_MIO(heuristicsForSQLAdvanced: Boolean){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN, heuristicsForSQLAdvanced)
    }


    private fun testRunEM(strategy : EMConfig.SecondaryObjectiveStrategy, heuristicsForSQLAdvanced: Boolean) {
        val outputFolder = "GQL_DirectIntEM_$strategy"
        val outputTestName = "org.foo.graphql.DirectIntEM_$strategy" + if (heuristicsForSQLAdvanced) "Complete" else "Partial"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputTestName,
            7000
        ) { args: MutableList<String> ->

            setOption(args,"problemType", EMConfig.ProblemType.GRAPHQL.toString())

            setOption(args, "secondaryObjectiveStrategy", strategy.toString())

            setOption(args, "heuristicsForSQL", "true")
            setOption(args, "generateSqlDataWithSearch", "false")
            setOption(args, "heuristicsForSQLAdvanced", if (heuristicsForSQLAdvanced) "true" else "false")

            setOption(args, "probOfSmartSampling", "0.0") // on this example, it has huge negative impact

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val nonEmptyReturn = listOf("\"x\":42","\"y\":77", "\"id\":")

            assertHasAtLeastOne(solution, "addDbDirectInt", GQMethodType.MUTATION, 200, nonEmptyReturn, false)
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, nonEmptyReturn, false)
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, "\"get\":[]")
            assertNoneWithErrors(solution)

            assertTextInTests(outputFolder, outputTestName, "controller.resetDatabase(")
            val condition: Predicate<String> = Predicate { it!!.contains("db_direct_int", true) }
            assertTextInTests(outputFolder, outputTestName, condition)
        }
    }
}
