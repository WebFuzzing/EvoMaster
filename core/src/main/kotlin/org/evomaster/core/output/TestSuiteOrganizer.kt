package org.evomaster.core.output

import org.evomaster.core.EMConfig
import org.evomaster.core.output.naming.TestCaseNamingStrategyFactory
import org.evomaster.core.output.sorting.SortingHelper
import org.evomaster.core.search.Solution


class TestSuiteOrganizer(
    private val config: EMConfig
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
     */
    fun createSortedTestCases(solution: Solution<*>): List<TestCase> {

        val namingStrategy = TestCaseNamingStrategyFactory(config).create(solution)

        val tests = namingStrategy.getTestCases()

        return sortingHelper.sort(tests, config.testCaseSortingStrategy)
    }


}




