package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.utils.StringUtils

open class RPCActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
    maxTestCaseNameLength: Int,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength) {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolvers: List<AmbiguitySolver>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RPCCallAction
        var remainingNameChars = maxTestCaseNameLength - namePrefixChars()

        remainingNameChars = addNameTokensIfAllowed(nameTokens, listOf(safeVariableName(action.getSimpleClassName()), on, safeVariableName(action.getExecutedFunctionName())), remainingNameChars)
        addResult(individual, nameTokens, remainingNameChars)

        return formatName(nameTokens)
    }

    override fun resolveAmbiguities(duplicatedIndividuals: Set<EvaluatedIndividual<*>>): Map<EvaluatedIndividual<*>, String> {
        // TODO do nothing at the moment. This will be completed with the experimental params disambiguation method
        return emptyMap()
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>, remainingNameChars: Int): Int {
        val result = evaluatedAction.result as RPCCallResult
        if (result.hasPotentialFault()) {
            val candidateTokens = mutableListOf(throws)
            val thrownException = StringUtils.extractSimpleClass(result.getExceptionTypeName()?: "")
            candidateTokens.add(safeVariableName(thrownException))
            return addNameTokensIfAllowed(nameTokens, candidateTokens, remainingNameChars)
        } else {
            val candidateTokens = mutableListOf(returns)
            candidateTokens.add(when {
                result.failedCall() -> error
                else -> success
            })
            return addNameTokensIfAllowed(nameTokens, candidateTokens, remainingNameChars)
        }
    }

}
