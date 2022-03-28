package org.evomaster.e2etests.spring.graphql.db.directint

import com.foo.graphql.db.directint.DbDirectIntController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*

class DbDirectIntEMTest : SpringTestBase() {


    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DbDirectIntController())
        }
    }

    @Test
    fun testAvg(){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE)
    }

    @Test
    fun testAvg_SAME_N(){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE_SAME_N_ACTIONS)
    }

    @Test
    fun testBest_MIO(){
        testRunEM(EMConfig.SecondaryObjectiveStrategy.BEST_MIN)
    }


    private fun testRunEM(strategy : EMConfig.SecondaryObjectiveStrategy) {
        val outputFolder = "GQL_DirectIntEM_$strategy"
        val outputTestName = "org.foo.graphql.DirectIntEM_$strategy"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputTestName,
            7000
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--secondaryObjectiveStrategy")
            args.add("" + strategy)
            args.add("--heuristicsForSQL")
            args.add("true")
            args.add("--generateSqlDataWithSearch")
            args.add("false")
            args.add("--probOfSmartSampling")
            args.add("0.0") // on this example, it has huge negative impact


            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val nonEmptyReturn = listOf("\"x\":42","\"y\":77", "\"id\":")

            assertHasAtLeastOne(solution, "addDbDirectInt", GQMethodType.MUTATION, 200, nonEmptyReturn, false)
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, nonEmptyReturn, false)
            assertHasAtLeastOne(solution, "get", GQMethodType.QUERY, 200, "\"get\":[]")
            assertNoneWithErrors(solution)

            assertTextInTests(outputFolder, outputTestName,
                "controller.resetDatabase(listOf(\"db_direct_int\"))"
            )
        }
    }
}