package org.evomaster.e2etests.spring.graphql.splitter

import com.foo.graphql.splitter.SplitterController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Termination
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.search.Solution
import org.evomaster.e2etests.spring.graphql.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class GHLSplitterEMTest : SpringTestBase() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(SplitterController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "GQLSplitter_SplitterEM",
                "org.foo.graphql.SplitterEM",
                listOf(Termination.SUCCESSES.suffix, Termination.FAULTS.suffix),
                1000
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
            //args.add("--testSuiteSplitType")
            //args.add("CODE")

            val solution = initAndRun(args)

            val em = EMConfig()

            em.testSuiteSplitType = EMConfig.TestSuiteSplitType.FAULTS
            //em.executiveSummary = false


            val splits = TestSuiteSplitter.split(solution, em)

            Assertions.assertTrue(solution.individuals.size >= 1)
            Assertions.assertTrue(splits.splitOutcome.get(0).individuals.size >= 1)
            Assertions.assertTrue(splits.splitOutcome.get(1).individuals.size >= 1)

            assertAnyWithErrors(splits.splitOutcome.get(0) as Solution<GraphQLIndividual>)
            //assertAnyWithErrors(solution)

            //existErrorAndSuccessTarget(targetFile)
            //ErrorsInStatisticsUtil.checkErrorsInStatistics(statFile, 1, 1, 1, -1, 1)


        }
    }
    private fun existErrorAndSuccessTarget(path : String){
        val file = File(path)
        Assertions.assertTrue(file.exists())

        val targets = file.readText()
        Assertions.assertTrue(targets.contains("GQL_ERRORS_ACTION:") && targets.contains("GQL_NO_ERRORS:") && targets.contains("GQL_ERRORS_LINE"), targets)
    }


}