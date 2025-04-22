package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class GraphQLActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    maxTestCaseNameLength: Int,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength) {


    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<AmbiguitySolver>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as GraphQLAction
        var remainingNameChars = maxTestCaseNameLength - namePrefixChars()

        remainingNameChars = addNameTokensIfAllowed(nameTokens, listOf(action.methodType.toString().lowercase(), on, safeVariableName(action.methodName)), remainingNameChars)
        addResult(individual, nameTokens, remainingNameChars)

        return formatName(nameTokens)
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        // TODO do nothing at the moment. This will be completed with the experimental params disambiguation method
        return emptyMap()
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>, remainingNameChars: Int): Int {
        val result = evaluatedAction.result as GraphQlCallResult
        val candidateTokens = mutableListOf(returns)
        candidateTokens.add(
            when {
                result.hasErrors() -> error
                result.hasNonEmptyData() -> data
                else -> empty
            }
        )
        return addNameTokensIfAllowed(nameTokens, candidateTokens, remainingNameChars)
    }

}
