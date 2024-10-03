package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.search.Solution

open class ActionTestCaseNamingStrategy(
    solution: Solution<*>,
    protected val languageConventionFormatter: LanguageConventionFormatter,
    protected val nameTokens: List<String> = listOf(),
) : NumberedTestCaseNamingStrategy(solution)  {

    protected fun formatName(): String {
        return "_${languageConventionFormatter.formatName(nameTokens)}"
    }

    protected fun getPath(nameQualifier: String): String {
        if (nameQualifier == "/") {
            return "root"
        }
        return TestWriterUtils.safeVariableName(nameQualifier)
    }

    protected fun fault(faults: Set<FaultCategory>): String {
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

}
