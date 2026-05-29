package org.evomaster.core.output

import org.evomaster.core.output.naming.TestCaseNamingStrategy
import org.evomaster.core.output.sorting.SortingHelper
import org.evomaster.core.output.sorting.SortingStrategy


class TestSuiteOrganizer {

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
     * Note that the 'solution' is stored insider the [namingStrategy]
     */
    fun createSortedTestCases(namingStrategy: TestCaseNamingStrategy, testCaseSortingStrategy: SortingStrategy): List<TestCase> {

        val tests = namingStrategy.getTestCases()

        return sortingHelper.sort(tests, testCaseSortingStrategy)
    }


}




