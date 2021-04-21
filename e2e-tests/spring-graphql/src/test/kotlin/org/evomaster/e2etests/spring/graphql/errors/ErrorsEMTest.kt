package org.evomaster.e2etests.spring.graphql.errors

import com.foo.graphql.errors.ErrorsController
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class ErrorsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ErrorsController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_ErrorsEM",
            "org.foo.graphql.ErrorsEM",
            100
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--exportCoveredTarget")
            args.add("true")

            val targetFile = "target/covered-targets/errors.txt"
            args.add("--coveredTargetFile")
            args.add(targetFile)

            val statFile = "target/statistics/errors_statistics.csv"
            args.add("--writeStatistics")
            args.add("true")
            args.add("--statisticsFile")
            args.add(statFile)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertAnyWithErrors(solution)

            existErrorAndSuccessTarget(targetFile)
            checkErrorsInStatistics(statFile, 1, 1)
        }
    }

    private fun existErrorAndSuccessTarget(path : String){
        val file = File(path)
        assertTrue(file.exists())

        val targets = file.readText()
        assertTrue(targets.contains("GQL_ERRORS_ACTION:") && targets.contains("GQL_NO_ERRORS:") && targets.contains("GQL_ERRORS_LINE"), targets)
    }

    private fun checkErrorsInStatistics(path: String, num: Int, numLine: Int){
        val file = File(path)
        assertTrue(file.exists())

        val stats = file.readLines()
        assertEquals(2, stats.size)
        val index = stats.first().split(",").indexOf("gqlerrors")
        val actual = stats[1].split(",")[index].toInt()
        assertEquals(num, actual)

        val indexline = stats.first().split(",").indexOf("gqlerrorsPerLines")
        val actualLine = stats[1].split(",")[indexline].toInt()
        assertEquals(numLine, actualLine)
    }

}