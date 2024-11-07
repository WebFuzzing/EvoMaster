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
    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)?): String {
        return ""
    }

    override fun resolveAmbiguities(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        // do nothing, plain numbered strategy will never have duplicate names
        return emptyMap()
    }

    // kicking off with an empty mutableListOf for each test case to accumulate their own name tokens
    private fun getName(counter: Int, individual: EvaluatedIndividual<*>): String {
        return "test_${counter}${expandName(individual, mutableListOf())}"
    }

    private fun concatName(counter: Int, expandedName: String): String {
        return "test_${counter}${expandedName}"
    }

    private fun generateNames(individuals: List<EvaluatedIndividual<*>>) : List<TestCase> {
        val individualToName = mutableMapOf<EvaluatedIndividual<*>, String>()
        individuals.forEach {
            // kicking off with an empty mutableListOf for each test case to accumulate their own name tokens
            individualToName[it] = expandName(it, mutableListOf())
        }

        getDuplicateNames(individualToName).forEach {
            var previousSize: Int
            do {
                previousSize = it.size
                individualToName.putAll(resolveAmbiguities(it))
            } while(previousSize != it.size)
        }

        var counter = 0
        return individualToName.map { entry -> TestCase(entry.key, concatName(counter++, entry.value)) }
    }

    private fun getDuplicateNames(individualToName: Map<EvaluatedIndividual<*>, String>): List<MutableSet<EvaluatedIndividual<*>>> {
        return individualToName
            .entries
            .groupBy({ it.value }, { it.key })
            .mapValues { (_, values) -> values.toMutableSet() }
            .filterValues { it.size > 1 }
            .values
            .toList()
    }

}
