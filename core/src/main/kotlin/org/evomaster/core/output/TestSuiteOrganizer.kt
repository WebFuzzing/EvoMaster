package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.llm.service.LlmService
import org.evomaster.core.output.naming.TestCaseNamingStrategyFactory
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.sorting.SortingHelper
import org.evomaster.core.search.Solution


class TestSuiteOrganizer(
    private val config: EMConfig,
    private val llmService: LlmService
) {

    private val sortingHelper = SortingHelper()


    /**
     * This method is responsible to decide the order in which
     * the test cases should be written in the final test suite.
     * Ideally, the most "interesting" tests should be written first.
     *
     * <br>
     *
     * Furthermore, this class is also responsible for deciding which
     * name each test will have.
     *
     * <br>
     * WARNING: side-effect of sorting tests inside input [solution] object
     *
     */
    fun createSortedTestCases(solution: Solution<*>, testCaseWriter: TestCaseWriter): List<TestCase> {

        /*
            Tests MUST be sorted before they are named, as their position might influence
            their name (eg, "test_0_...")
         */
        sortingHelper.sort(solution.individuals, config.testCaseSortingStrategy)

        val namingStrategy = TestCaseNamingStrategyFactory(config, testCaseWriter, llmService).create(solution)

        val tests = namingStrategy.getTestCases()

        return tests
    }


}




