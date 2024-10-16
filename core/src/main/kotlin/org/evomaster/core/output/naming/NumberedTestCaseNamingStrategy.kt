package org.evomaster.core.output.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action

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
    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> String)?): String {
        return ""
    }

    override fun resolveAmbiguity(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: Set<EvaluatedIndividual<*>>) {
        // do nothing, plain numbered strategy will never have duplicate names
    }

    private fun concatName(counter: Int, expandedName: String): String {
        return "test_${counter}${expandedName}"
    }

    private fun generateNames(individuals: List<EvaluatedIndividual<*>>) : List<TestCase> {
        val individualToName = mutableMapOf<EvaluatedIndividual<*>, String>()
        individuals.forEach {
            // kicking off with an empty mutableListOf for each test case to accumulate their own name tokens
            ind -> individualToName[ind] = expandName(ind, mutableListOf())
        }

        val duplicatedNames = getDuplicateNames(individualToName)
        duplicatedNames.forEach { entry -> resolveAmbiguity(individualToName, entry.value) }

        var counter = 0
        return individualToName.map { entry -> TestCase(entry.key, concatName(counter++, entry.value)) }
    }

    private fun getDuplicateNames(individualToName: Map<EvaluatedIndividual<*>, String>): Map<String, Set<EvaluatedIndividual<*>>> {
        val result = mutableMapOf<String, MutableSet<EvaluatedIndividual<*>>>()
        individualToName.forEach { entry ->
            val testName = entry.value
            if (!result.containsKey(testName)) {
                val inds = mutableSetOf<EvaluatedIndividual<*>>()
                inds.add(entry.key)
                result[testName] = inds
            } else {
                result[testName]?.add(entry.key)
            }
        }
        result.entries.removeIf { it.value.size <= 1 }
        return result
    }

}
