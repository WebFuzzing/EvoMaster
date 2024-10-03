package org.evomaster.core.output.naming

import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class RestActionTestCaseNamingStrategy(
    solution: Solution<*>,
    languageConventionFormatter: LanguageConventionFormatter
) : ActionTestCaseNamingStrategy(solution, languageConventionFormatter)  {


    override fun expandName(individual: EvaluatedIndividual<*>): String {
        var evaluatedAction = individual.evaluatedMainActions().last()
        var action = evaluatedAction.action as RestCallAction

        return "_${languageConventionFormatter.formatName(listOf(action.verb.toString(), "on", getPath(action.path.nameQualifier), addResult(individual)))}"
    }

    private fun addResult(individual: EvaluatedIndividual<*>): String {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        if (detectedFaults.isNotEmpty()) {
            return fault(detectedFaults)
        }
        return statusCode(individual.evaluatedMainActions().last())
    }

    private fun statusCode(evaluatedAction: EvaluatedAction): String {
        var result = evaluatedAction.result as HttpWsCallResult
        return "returns_${result.getStatusCode()}"
    }

}
