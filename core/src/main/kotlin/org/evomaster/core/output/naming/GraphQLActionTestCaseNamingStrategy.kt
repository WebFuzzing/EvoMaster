package org.evomaster.core.output.naming

import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class GraphQLActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {


    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as GraphQLAction

        nameTokens.add(action.methodType.toString())
        nameTokens.add(on)
        nameTokens.add(getPath(action.methodName))
        addResult(individual, nameTokens)

        return formatName(nameTokens)
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>) {
        val result = evaluatedAction.result as GraphQlCallResult
        nameTokens.add(returns)
        nameTokens.add(
            when {
                result.hasErrors() -> error
                result.hasNonEmptyData() -> data
                else -> empty
            }
        )
    }

}
