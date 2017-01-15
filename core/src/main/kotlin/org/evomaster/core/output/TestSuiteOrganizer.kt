package org.evomaster.core.output

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

    companion object {
        fun sortTests(solution: Solution<*>): List<TestCase> {

            //TODO here in the future we will have something bit smarter
            return naiveSorting(solution)
        }

        /**
         * No sorting, and just basic name with incremental counter
         */
        private fun naiveSorting(solution: Solution<*>): List<TestCase> {

            var counter = 0

            return solution.individuals.map { ind -> TestCase(ind, "test" + (counter++)) }
        }
    }
}