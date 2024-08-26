package org.evomaster.e2etests.spring.graphql.errors

import com.foo.graphql.onlyerrors.OnlyErrorsController
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class OnlyErrorsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(OnlyErrorsController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "GQL_OnlyErrorsEM",
            "org.foo.graphql.OnlyErrorsEM",
            100
        ) { args: MutableList<String> ->

            args.add("--problemType")
            args.add(EMConfig.ProblemType.GRAPHQL.toString())

            args.add("--exportCoveredTarget")
            args.add("true")

            val targetFile = "target/covered-targets/onlyerrors.txt"
            args.add("--coveredTargetFile")
            args.add(targetFile)

            val statFile = "target/statistics/onlyerrors_statistics.csv"
            args.add("--writeStatistics")
            args.add("true")
            args.add("--statisticsFile")
            args.add(statFile)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertAnyWithErrors(solution)

            existErrorAndSuccessTarget(targetFile)
            ErrorsInStatisticsUtil.checkErrorsInStatistics(statFile, 1, 1, 1, 0, 1)
        }
    }

    private fun existErrorAndSuccessTarget(path : String){
        val file = File(path)
        assertTrue(file.exists())

        val targets = file.readText()
        assertTrue(targets.contains(FaultCategory.GQL_ERROR_FIELD.code.toString()))
    }

}