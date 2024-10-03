package org.evomaster.core.output.naming

import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class GraphQLActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {


    override fun expandName(individual: EvaluatedIndividual<*>): String {
        var evaluatedAction = individual.evaluatedMainActions().last()
        var action = evaluatedAction.action as GraphQLAction

        nameTokens.plus(action.methodType.toString(), "on", )
        nameTokens.plus(getPath(action.methodName))
        nameTokens.plus(addResult(individual))

        return formatName()
    }

    private fun addResult(individual: EvaluatedIndividual<*>) {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        if (detectedFaults.isNotEmpty()) {
            nameTokens.plus(fault(detectedFaults))
        }
        return addGraphQLResult(individual.evaluatedMainActions().last())
    }

    private fun addGraphQLResult(evaluatedAction: EvaluatedAction): List<String> {
        val result = evaluatedAction.result as GraphQlCallResult
        val ret = listOf("returns")
        ret.plus(
            when {
                result.hasErrors() -> "error"
                result.hasNonEmptyData() -> "data"
                else -> "empty"
            }
        )
        return ret
    }

}
