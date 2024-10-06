package org.evomaster.core.output.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution

open class NumberedTestCaseNamingStrategy(
    solution: Solution<*>
) : TestCaseNamingStrategy(solution) {

    override fun getTestCases(): List<TestCase> {
        return generateNames(solution.individuals)
    }

    override fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase> {
        val inds = solution.individuals
        comparators.asReversed().forEach {
            inds.sortWith(it)
        }
        return generateNames(inds)
    }

    // numbered strategy will not expand the name unless it is using the namingHelper
    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>): String {
        return ""
    }

    // kicking off with an empty mutableListOf for each test case to accumulate their own name tokens
    private fun getName(counter: Int, individual: EvaluatedIndividual<*>): String {
        return "test_${counter}${expandName(individual, mutableListOf())}"
    }

    private fun generateNames(individuals: List<EvaluatedIndividual<*>>) : List<TestCase> {
        var counter = 0
        return individuals.map { ind -> TestCase(ind, getName(counter++, ind)) }
    }

}
