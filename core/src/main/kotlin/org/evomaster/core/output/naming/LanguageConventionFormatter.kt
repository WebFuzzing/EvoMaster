package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.utils.StringUtils.capitalization

/**
 * Different programming languages have different conventions and style when defining variable names and other programming structures.
 * As such, when we generate tests, ideally we would like to follow the same conventions.
 *
 * JVM languages usually use camelCase
 * Python uses snake_case
 * JavaScript uses PascalCase
 */
class LanguageConventionFormatter(
    private val outputFormat: OutputFormat
) {

    fun formatName(testKeywords: List<String>): String {
        return when {
            outputFormat.isJavaOrKotlin() -> formatCamelCase(testKeywords)
            outputFormat.isPython() -> formatSnakeCase(testKeywords)
            outputFormat.isJavaScript() -> formatPascalCase(testKeywords)
            else -> throw IllegalStateException("Output format $outputFormat does not have a language formatter set for test case naming")
        }

    }

    private fun formatCamelCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("") { capitalization(it) }.decapitalize()
    }

    private fun formatSnakeCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("_") { it }
    }

    private fun formatPascalCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("") { capitalization(it) }
    }

}
