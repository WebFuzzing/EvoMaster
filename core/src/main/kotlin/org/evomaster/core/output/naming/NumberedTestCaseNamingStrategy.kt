package org.evomaster.core.output.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import java.util.Collections.singletonList

open class NumberedTestCaseNamingStrategy(
    solution: Solution<*>
) : TestCaseNamingStrategy(solution) {

    override fun getTestCases(): List<TestCase> {
        return generateNames(solution.individuals)
    }

    override fun getSortedTestCases(comparator: Comparator<EvaluatedIndividual<*>>): List<TestCase> {
        return getSortedTestCases(singletonList(comparator))
    }

    override fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase> {
        val inds = solution.individuals

        comparators.asReversed().forEach {
            inds.sortWith(it)
        }

        return generateNames(inds)
    }

    // numbered strategy will not expand the name unless it is using the namingHelper
    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<AmbiguitySolver>): String {
        return ""
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        // do nothing, plain numbered strategy will never have duplicate names
        return emptyMap()
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
                val solvedAmbiguities = resolveAmbiguities(it)
                individualToName.putAll(solvedAmbiguities)
                removeSolvedDuplicates(it, solvedAmbiguities.keys)
            } while(previousSize != it.size)
        }

        var counter = 0
        return individualToName.map { entry -> TestCase(entry.key, concatName(counter++, entry.value)) }
    }

    private fun removeSolvedDuplicates(duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>, disambiguatedIndividuals: Set<EvaluatedIndividual<*>>) {
        duplicatedIndividuals.removeAll(disambiguatedIndividuals)
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
