package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.httpws.HttpWsCallResult
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

open class ActionTestCaseNamingStrategy(
    solution: Solution<*>,
    private val languageConventionFormatter: LanguageConventionFormatter
) : NumberedTestCaseNamingStrategy(solution)  {


    override fun expandName(individual: EvaluatedIndividual<*>): String {
        var evaluatedAction = individual.evaluatedMainActions().last()
        var action = evaluatedAction.action as RestCallAction

        return "_${languageConventionFormatter.formatName(listOf(action.verb.toString(), "on", getPath(action.path.nameQualifier), addResult(individual)))}"
    }

    private fun getPath(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return "root"
        }
        return TestWriterUtils.safeVariableName(nameQualifier)
    }

    private fun addResult(individual: EvaluatedIndividual<*>): String {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        if (detectedFaults.isNotEmpty()) {
            return fault(detectedFaults)
        }
        return statusCode(individual.evaluatedMainActions().last())
    }

    private fun fault(faults: Set<FaultCategory>): String {
        if (faults.size > 1) {
            var faultCodes = StringBuilder("showsFaults")
            faults.sortedBy { it.code }.forEach { fault -> faultCodes.append("_${fault.code}") }
            return faultCodes.toString()
        }
        return faults.first().testCaseLabel
    }

    private fun statusCode(evaluatedAction: EvaluatedAction): String {
        var result = evaluatedAction.result as HttpWsCallResult
        return "returns_${result.getStatusCode()}"
    }

}
