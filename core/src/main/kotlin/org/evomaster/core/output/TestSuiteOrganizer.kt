package org.evomaster.core.output

import org.evomaster.core.output.naming.TestCaseNamingStrategy
import org.evomaster.core.output.sorting.SortingHelper
import org.evomaster.core.output.sorting.SortingStrategy
import org.evomaster.core.search.Solution

/**
 * This class is responsible to decide the order in which
 * the test cases should be written in the final test suite.
 * Ideally, the most "interesting" tests should be written first.
 *
 * <br>
 *
 * Furthermore, this class is also responsible for deciding which
 * name each test will have
 */
class TestSuiteOrganizer {

    private val sortingHelper = SortingHelper()


    fun sortTests(solution: Solution<*>, namingStrategy: TestCaseNamingStrategy, testCaseSortingStrategy: SortingStrategy): List<TestCase> {
        return sortingHelper.sort(solution, namingStrategy, testCaseSortingStrategy)
    }
}




