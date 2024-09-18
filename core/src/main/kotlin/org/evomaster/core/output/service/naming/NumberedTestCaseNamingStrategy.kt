package org.evomaster.core.output.service.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

class NumberedTestCaseNamingStrategy(
    val solution: Solution<*>
) : TestCaseNamingStrategy {

    override fun getTestCases(): List<TestCase> {
        var counter = 0
        return solution.individuals.map { ind -> TestCase(ind, "test_${counter++}") }
    }

    override fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase> {
        val inds = solution.individuals
        comparators.asReversed().forEach {
            //solution.individuals.sortWith(it)
            inds.sortWith(it)
        }
        TODO("Not yet implemented")
    }

}
