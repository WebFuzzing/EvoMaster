package org.evomaster.core.output.naming

import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action

/**
 * A naming strategy will help provide names to the generated test cases.
 * Naming can rely upon information contained in the solution provided.
 */
abstract class TestCaseNamingStrategy(
    protected val solution: Solution<*>
) {

    /**
     * @return the list of TestCase with the generated name given the naming strategy
     */
    abstract fun getTestCases(): List<TestCase>

    /**
     * @param comparators used to sort the test cases
     *
     * @return the list of sorted TestCase with the generated name given the naming strategy
     */
    abstract fun getSortedTestCases(comparators: List<Comparator<EvaluatedIndividual<*>>>): List<TestCase>

    /**
     * @param individual containing information for the test about to be named
     * @param nameTokens list to collect the identifiers which will be formatted into the test case name
     * @param ambiguitySolver function receiving an action and returning a list of strings that will be added to the  test case name
     *
     * @return a String with extra information that will be included in the test name, regarding the EvaluatedIndividual
     */
    protected abstract fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)? = null): String

    /**
     * @param individualToName map pairing each EvaluatedIndividual to the generated name
     * @param duplicatedIndividuals set containing the EvaluatedIndividuals sharing the same name
     */
    protected abstract fun resolveAmbiguity(individualToName: MutableMap<EvaluatedIndividual<*>, String>, duplicatedIndividuals: MutableSet<EvaluatedIndividual<*>>)

}
