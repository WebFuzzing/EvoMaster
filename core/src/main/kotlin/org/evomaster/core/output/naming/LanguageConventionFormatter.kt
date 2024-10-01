package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat

class LanguageConventionFormatter(
    private val outputFormat: OutputFormat
) {

    fun formatName(testKeywords: List<String>): String {
        return when {
            outputFormat.isJavaOrKotlin() -> formatCamelCase(testKeywords)
            outputFormat.isPython() -> formatSnakeCase(testKeywords)
            outputFormat.isJavaScript() -> formatPascalCase(testKeywords)
            else -> formatCamelCase(testKeywords)
        }

    }

    private fun formatCamelCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("") { capitalize(it) }.decapitalize()
    }

    private fun formatSnakeCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("_") { it }
    }

    private fun formatPascalCase(testKeywords: List<String>): String {
        return testKeywords.joinToString("") { capitalize(it) }
    }

    fun capitalize(word: String): String {
        return word[0].uppercaseChar() + word.substring(1).lowercase()
    }

}
