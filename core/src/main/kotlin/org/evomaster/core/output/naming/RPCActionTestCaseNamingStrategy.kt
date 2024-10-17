package org.evomaster.core.output.naming

import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestWriterUtils.safeVariableName
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EvaluatedAction
import org.evomaster.core.utils.StringUtils

open class RPCActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {

    override fun expandName(individual: EvaluatedIndividual<*>, nameTokens: MutableList<String>, ambiguitySolver: ((Action) -> List<String>)?): String {
        val evaluatedAction = individual.evaluatedMainActions().last()
        val action = evaluatedAction.action as RPCCallAction

        nameTokens.add(safeVariableName(action.getSimpleClassName()))
        nameTokens.add(on)
        nameTokens.add(safeVariableName(action.getExecutedFunctionName()))
        if (ambiguitySolver != null) {
            nameTokens.addAll(ambiguitySolver(action))
        }
        addResult(individual, nameTokens)

        return formatName(nameTokens)
    }

    override fun addActionResult(evaluatedAction: EvaluatedAction, nameTokens: MutableList<String>) {
        val result = evaluatedAction.result as RPCCallResult
        if (result.hasPotentialFault()) {
            nameTokens.add(throws)
            val thrownException = StringUtils.extractSimpleClass(result.getExceptionTypeName()?: "")
            nameTokens.add(safeVariableName(thrownException))
        } else {
            nameTokens.add(returns)
            nameTokens.add(when {
                result.failedCall() -> error
                else -> success
            })
        }
    }

    override fun resolveAmbiguity(individualToName: MutableMap<EvaluatedIndividual<*>, String>, inds: MutableSet<EvaluatedIndividual<*>>) {
        inds.forEach { ind ->
            individualToName[ind] = expandName(ind, mutableListOf(), ::paramsAmbiguitySolver)
            inds.remove(ind)
        }
    }

    private fun paramsAmbiguitySolver(action: Action): List<String> {
        val rpcCallAction = action as RPCCallAction
        val result = mutableListOf<String>()

        val params = rpcCallAction.parameters.filterIsInstance<RPCParam>()
        result.add(with)
        val withParams = StringBuilder(param)
        if (params.size > 1) withParams.append("s")

        params.forEach { param -> withParams.append("_${safeVariableName(param.primaryGene().getValueAsRawString())}") }

        result.add(withParams.append("_").toString())
        return result
    }

}
