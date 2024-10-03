package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class RPCActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>): String {
        var evaluatedAction = individual.evaluatedMainActions().last()
        var action = evaluatedAction.action as RPCCallAction

        nameTokens.add(action.interfaceId)
        nameTokens.add(action.id)
        addResult(individual)

        return formatName()
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction) {
        val result = evaluatedAction.result as RPCCallResult
        if (result.hasPotentialFault()) {
            nameTokens.add(throws)
            nameTokens.add(TestWriterUtils.safeVariableName(result.getExceptionInfo()))
        } else {
            nameTokens.add(returns)
            nameTokens.add(when {
                result.failedCall() -> error
                else -> success
            })
        }
    }

}
