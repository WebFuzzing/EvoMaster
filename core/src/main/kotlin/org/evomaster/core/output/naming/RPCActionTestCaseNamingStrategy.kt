package org.evomaster.core.output.naming

import org.evomaster.core.EMConfig
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class RPCActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter,
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RPCCallAction

        nameTokens.add(TestWriterUtils.safeVariableName(action.getSimpleClassName()))
        nameTokens.add(on)
        nameTokens.add(TestWriterUtils.safeVariableName(action.getExecutedFunctionName()))
        addResult(individual, nameTokens)

        return formatName(nameTokens)
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>) {
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
