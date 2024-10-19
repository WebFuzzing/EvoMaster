package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LanguageConventionFormatterTest {

    @Test
    fun testFormatToCamelCase() {
        val testKeywords = listOf("get", "on", "root", "returns", "200")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)

        assertEquals("getOnRootReturns200", languageConventionFormatter.formatName(testKeywords))
    }

    @Test
    fun testFormatToSnakeCase() {
        val testKeywords = listOf("GET", "on", "root", "returns", "200")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        assertEquals("GET_on_root_returns_200", languageConventionFormatter.formatName(testKeywords))
    }

    @Test
    fun testFormatToPascalCase() {
        val testKeywords = listOf("GET", "on", "root", "returns", "200")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JS_JEST)

        assertEquals("GetOnRootReturns200", languageConventionFormatter.formatName(testKeywords))
    }
}
