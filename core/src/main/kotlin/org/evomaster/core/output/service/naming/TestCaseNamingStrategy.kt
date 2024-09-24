package org.evomaster.core.output.service.naming

import org.evomaster.core.output.NamingHelper
import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

abstract class TestCaseNamingStrategy(
    protected val solution: Solution<*>
) {

    abstract fun getTestCases(): List<TestCase>

    abstract fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>, namingHelper: NamingHelper): List<TestCase>

}
