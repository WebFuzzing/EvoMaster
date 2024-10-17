package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.graphql.param.GQReturnParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EvaluatedAction

open class GraphQLActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {


    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)?): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as GraphQLAction

        nameTokens.add(action.methodType.toString())
        nameTokens.add(on)
        nameTokens.add(getPath(action.methodName))
        if (ambiguitySolver != null) {
            nameTokens.addAll(ambiguitySolver(action))
        }
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

    override fun resolveAmbiguity(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        inds.forEach { ind ->
            individualToName[ind] = expandName(ind, mutableListOf(), ::paramsAmbiguitySolver)
        }
    }

    private fun paramsAmbiguitySolver(action: Action): List<String> {
        val graphQLAction = action as GraphQLAction
        val result = mutableListOf<String>()

        val params = graphQLAction.parameters.filter { p -> p is GQInputParam || p is GQReturnParam }
        result.add(with)
        val withParams = StringBuilder(param)
        if (params.size > 1) withParams.append("s")

        params.forEach { param ->
            val paramValue = param.primaryGene().getValueAsRawString()
            if (!paramValue.isNullOrEmpty()) withParams.append("_${safeVariableName(paramValue)}")
        }

        result.add(withParams.append("_").toString())
        return result
    }

}
