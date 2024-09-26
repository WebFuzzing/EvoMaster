package org.evomaster.core.output.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

open class NumberedTestCaseNamingStrategy(
    solution: Solution<*>
) : TestCaseNamingStrategy(solution) {


    override fun generateNames(individuals: List<EvaluatedIndividual<*>>) {
        var counter = 0
        individuals.forEach { ind ->
            testCaseNames[ind] = "test_${counter++}${expandName(ind)}"
        }
    }

    // numbered strategy will not expand the name unless it is using the namingHelper
    override fun expandName(individual: EvaluatedIndividual<*>): String {
        return ""
    }

    override fun getTestCases(): List<TestCase> {
        generateNames(solution.individuals)
        return testCaseNames.map { entry -> TestCase(entry.key, entry.value) }
    }

    override fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase> {
        val inds = solution.individuals
        comparators.asReversed().forEach {
            inds.sortWith(it)
        }
        generateNames(inds)
        return testCaseNames.map { entry -> TestCase(entry.key, entry.value) }
    }

}
