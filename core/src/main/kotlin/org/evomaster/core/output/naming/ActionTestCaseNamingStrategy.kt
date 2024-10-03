package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.EvaluatedAction

abstract class ActionTestCaseNamingStrategy(
    solution: Solution<*>,
    private val languageConventionFormatter: LanguageConventionFormatter,
    protected var nameTokens: MutableList<String> = mutableListOf(),
) : NumberedTestCaseNamingStrategy(solution)  {

    protected val on = "on"
    protected val throws = "throws"
    protected val returns = "returns"
    protected val error = "error"
    protected val success = "success"
    protected val data = "data"
    protected val empty = "empty"

    protected fun formatName(): String {
        return "_${languageConventionFormatter.formatName(nameTokens)}"
    }

    protected fun getPath(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return "root"
        }
        return TestWriterUtils.safeVariableName(nameQualifier)
    }

    private fun fault(faults: Set<FaultCategory>): String {
        if (faults.size > 1) {
            val faultCodes = StringBuilder("showsFaults")
            /*
              For better readability, multiple faults will be concatenated in a string separated by underscore
              to help understand it is a list of codes. Regardless of the outputFormat and language conventions.
             */
            faults.sortedBy { it.code }.forEach { fault -> faultCodes.append("_${fault.code}") }
            return faultCodes.toString()
        }
        return faults.first().testCaseLabel
    }

    protected fun addResult(individual: EvaluatedIndividual<*>) {
        val detectedFaults = DetectedFaultUtils.getDetectedFaultCategories(individual)
        if (detectedFaults.isNotEmpty()) {
            nameTokens.add(fault(detectedFaults))
        } else {
            addActionResult(individual.evaluatedMainActions().last())
        }
    }

    protected abstract fun addActionResult(evaluatedAction: EvaluatedAction)

}
