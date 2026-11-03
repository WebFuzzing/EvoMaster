package org.evomaster.core.output

import org.evomaster.core.output.service.PartialOracles
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FlakyContainerAssertionsTest : WriterTestBase() {

    private fun emit(format: OutputFormat, body: String, observed: String?): String {
        val writer = RestTestCaseWriter(getConfig(format), PartialOracles())
        val merged = observed?.let {
            val result = RestCallResult("action", false).apply { setBody(body) }
            result.recordFlakyObservation(RestCallResult("action", false).apply { setBody(it) }, 1)
            result.getMergedFlakyBody()
        }
        val lines = Lines(format)
        writer.handleJsonStringAssertion(body, merged, lines, null, false)
        return lines.toString()
    }

    private fun activeAssertions(text: String): List<String> =
        text.lineSequence().map { it.trim() }.filter { it.startsWith(".body(") }.toList()

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testArrayChangedToNonArrayDoesNotProduceArrayAssertions(format: OutputFormat) {
        for (original in listOf("[]", "[1,2]", "[\"one\"]", "[{\"x\":1}]")) {
            for (observed in listOf("0", "true", "\"changed\"", "{}", "null")) {
                val text = emit(format, """{"items":$original,"stable":7}""", """{"items":$observed,"stable":7}""")
                val active = activeAssertions(text)
                assertFalse(active.any { it.contains("items") }, text)
                assertTrue(active.any { it.contains("stable") && it.contains("numberMatches(7)") }, text)
                assertTrue(text.contains("Flaky type"), text)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testObjectChangedToNonObjectDoesNotProduceObjectAssertions(format: OutputFormat) {
        for (original in listOf("{}", "{\"x\":1}")) {
            for (observed in listOf("0", "true", "\"changed\"", "[]", "null")) {
                val text = emit(format, """{"item":$original,"stable":7}""", """{"item":$observed,"stable":7}""")
                assertFalse(activeAssertions(text).any { it.contains("item") }, text)
                assertTrue(activeAssertions(text).any { it.contains("stable") }, text)
                assertTrue(text.contains("Flaky type"), text)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testArrayElementChangedToNonArrayKeepsStableSibling(format: OutputFormat) {
        for (observed in listOf("1", "null", "{}")) {
            val text = emit(format, "[[1],7]", "[$observed,7]")
            val active = activeAssertions(text)
            assertFalse(active.any { it.contains("[0]") }, text)
            assertTrue(active.any { it.contains("[1]") && it.contains("numberMatches(7)") }, text)
            assertTrue(text.contains("Flaky type"), text)
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testReorderedErrorsDoNotRetainAnArrayAssertionAtAScalarPosition(format: OutputFormat) {
        val arrayError = """{"field":"a","arguments":[null,[]]}"""
        val scalarError = """{"field":"b","arguments":[null,1]}"""
        val body = """{"errors":[$arrayError,$scalarError],"stable":7}"""
        val observed = """{"errors":[$scalarError,$arrayError],"stable":7}"""
        val text = emit(format, body, observed)
        assertFalse(activeAssertions(text).any { it.contains("'errors'[0].'arguments'[1]") }, text)
        assertTrue(activeAssertions(text).any { it.contains("stable") }, text)
        assertTrue(text.contains("Flaky type"), text)
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun testNoObservationAndUnchangedContainersKeepTheirAssertions(format: OutputFormat) {
        for (body in listOf("""{"items":[1,2],"item":{"x":1}}""", """{"items":[],"item":{}}""", "[[1],7]")) {
            val withoutObservation = emit(format, body, null)
            assertTrue(activeAssertions(withoutObservation).isNotEmpty(), withoutObservation)
            assertEquals(withoutObservation, emit(format, body, body))
            assertFalse(withoutObservation.contains("Flaky type"), withoutObservation)
        }
    }
}
