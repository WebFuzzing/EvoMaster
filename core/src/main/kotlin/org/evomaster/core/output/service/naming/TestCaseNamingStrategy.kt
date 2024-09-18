package org.evomaster.core.output.service.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual

interface TestCaseNamingStrategy {

    fun getTestCases(): List<TestCase>

    fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase>

}
