package org.evomaster.core.output.naming

import org.evomaster.core.output.NamingHelper
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action

class NamingHelperNumberedTestCaseNamingStrategy(
    solution: Solution<*>
) : NumberedTestCaseNamingStrategy(solution) {

    private val namingHelper = NamingHelper()

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)?): String {
        return namingHelper.suggestName(individual)
    }
}
