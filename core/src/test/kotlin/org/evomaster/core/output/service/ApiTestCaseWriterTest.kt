package org.evomaster.core.output.service

import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.WriterTestBase
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.database.sql.SqlAction
import javax.ws.rs.core.MediaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** Verify that cumulative observations suppress only contradicted response assertions. */
class ApiTestCaseWriterTest : WriterTestBase() {

    private fun emit(format: OutputFormat, body: String, observations: List<String>, static: Boolean = false): String {
        val result = RestCallResult("action", false).apply { setBody(body) }
        if (static) result.recordStaticFlakyInference()
        observations.forEachIndexed { index, observation ->
            result.recordFlakyObservation(RestCallResult("action", false).apply { setBody(observation) }, index + 1)
        }
        val lines = Lines(format)
        RestTestCaseWriter(getConfig(format), PartialOracles()).handleJsonStringAssertions(
            body, result.getFlakyBodies().orEmpty(), lines, null, false)
        return lines.toString()
    }

    private fun active(text: String) = text.lineSequence().map { it.trim() }.filter { it.startsWith(".body(") }.toList()

    private fun permutations(values: List<String>): List<List<String>> =
        if (values.isEmpty()) listOf(emptyList())
        else values.indices.flatMap { index -> permutations(values.filterIndexed { i, _ -> i != index }).map { listOf(values[index]) + it } }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun completeHttpWriterUsesAllObservations(format: OutputFormat) {
        val action = RestCallAction("1", HttpVerb.GET, RestPath("/foo"), mutableListOf())
        val (_, baseUrl, individual) = EvaluatedIndividualBuilder.buildResourceEvaluatedIndividual(
            dbInitialization = mutableListOf(),
            groups = mutableListOf(mutableListOf<SqlAction>() to mutableListOf(action)), format = format)
        val result = individual.seeResult(action.getLocalId()) as RestCallResult
        result.setTimedout(false)
        result.setStatusCode(200)
        result.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        result.setBody("""{"items":[1,2],"stable":7}""")
        listOf("""{"items":0,"stable":7}""", """{"items":[3,2],"stable":7}""").forEachIndexed { i, body ->
            result.recordFlakyObservation(RestCallResult(action.getLocalId()).apply {
                setStatusCode(200)
                setBodyType(MediaType.APPLICATION_JSON_TYPE)
                setBody(body)
            }, i + 1)
        }
        val writer = RestTestCaseWriter(getConfig(format).apply { handleFlakiness = true }, PartialOracles())
        val text = writer.convertToCompilableTestCode(TestCase(individual, "test"), baseUrl).toString()
        assertFalse(active(text).any { it.contains("items") }, text)
        assertTrue(active(text).any { it.contains("stable") }, text)
        assertTrue(text.contains("// .body(\"'items'.size()\""), text)
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun typeChangeIsNeverForgotten(format: OutputFormat) {
        val original = """{"items":[1,2],"stable":7}"""
        val observations = listOf("""{"items":0,"stable":7}""", """{"items":[3,2],"stable":7}""", original)
        for (order in permutations(observations)) {
            val text = emit(format, original, order)
            assertEquals(listOf(".body(\"'stable'\", numberMatches(7))"), active(text), text)
            assertTrue(text.contains("// .body(\"'items'.size()\""), text)
            assertTrue(text.contains("// .body(\"'items'[1]\""), text)
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun missingFieldsCannotReappearAsStable(format: OutputFormat) {
        val body = """{"items":[{"x":1,"y":2}],"stable":7}"""
        val observations = listOf("""{"items":[{"x":1}],"stable":7}""", """{"items":[{"x":9,"y":2}],"stable":7}""")
        for (order in permutations(observations)) {
            val text = emit(format, body, order)
            assertFalse(active(text).any { it.contains("'x'") || it.contains("'y'") }, text)
            assertTrue(active(text).any { it.contains("stable") }, text)
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun membershipUsesRealArraysNotMergedElements(format: OutputFormat) {
        val body = """["A","B","C"]"""
        val observations = listOf("""["B","A","C"]""", """["C","B","A"]""", """["A","B","C","D"]""")
        for (order in permutations(observations)) {
            val text = emit(format, body, order)
            assertEquals(listOf(".body(\"\", hasItems(\"A\", \"B\", \"C\"))"), active(text), text)
        }
        val text = emit(format, body, listOf("""["A",1,"C"]"""))
        assertFalse(active(text).any { it.contains("hasItems") }, text)
        assertTrue(active(text).any { it.contains("size()") }, text)
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun nullAndMissingAreNotNoObservation(format: OutputFormat) {
        for (value in listOf("1", "true", "\"text\"", "[]", "{}", "[1]", "{\"x\":1}")) {
            val original = """{"v":$value,"stable":7}"""
            for (changed in listOf("""{"v":null,"stable":7}""", """{"stable":7}""")) {
                val text = emit(format, original, listOf(changed, original))
                assertFalse(active(text).any { it.contains("'v'") }, text)
                assertTrue(active(text).any { it.contains("stable") }, text)
            }
            val text = emit(format, """{"v":null}""", listOf("""{"v":$value}"""))
            assertTrue(active(text).isEmpty(), text)
        }
        assertTrue(active(emit(format, """{"v":null}""", listOf("""{"v":null}"""))).isNotEmpty())
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun rootAndParentChangesCommentDependentAssertions(format: OutputFormat) {
        for (original in listOf("[1,2]", "[]", "{}", """{"v":[1]}""")) {
            for (changed in listOf("null", "42", "\"text\"", "<html>error</html>", "")) {
                val text = emit(format, original, listOf(changed))
                assertTrue(active(text).isEmpty(), text)
                assertTrue(text.contains("// .body("), text)
            }
        }
        val text = emit(format, """{"v":{"x":[1]},"stable":7}""", listOf("""{"v":[],"stable":7}"""))
        assertFalse(active(text).any { it.contains("'v'") }, text)
        assertTrue(active(text).any { it.contains("stable") }, text)
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun shortenedAndEmptyArraysKeepValidAssertions(format: OutputFormat) {
        val text = emit(format, "[1,2,3]", listOf("[1,2]", "[1,9,3]"))
        assertEquals(listOf(".body(\"[0]\", numberMatches(1))"), active(text), text)
        assertTrue(text.contains("// .body(\"[2]\""), text)
        assertTrue(active(emit(format, "[]", listOf("[1]"))).isEmpty())
        assertTrue(active(emit(format, "{}", listOf("""{"a":1}"""))).isEmpty())
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun assertionSemanticsPreserveStablePredicates(format: OutputFormat) {
        val original = """{"text":"abc","number":1,"ok":true}"""
        assertEquals(active(emit(format, original, emptyList())),
            active(emit(format, original, listOf("""{"text":"abcd","number":1.0,"ok":true}"""))))
        assertTrue(active(emit(format, original, listOf("""{"text":"ab","number":2,"ok":false}"""))).isEmpty())
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun staticInferenceAndRepeatedObservationsBothApply(format: OutputFormat) {
        val body = """{"created":"2026-11-15","epoch":1794700000000,"v":1,"stable":7}"""
        val text = emit(format, body, listOf("""{"created":"2026-11-15","epoch":1794700000000,"v":2,"stable":7}"""), static = true)
        assertEquals(listOf(".body(\"'stable'\", numberMatches(7))"), active(text), text)
        assertEquals(4, active(emit(format, body, emptyList())).size)
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_5", "KOTLIN_JUNIT_5"])
    fun ocvnShapedConstraintArgumentsAreCheckedAtTheirOriginalPositions(format: OutputFormat) {
        // Minimized Spring validation response: EachPattern has an array of flags, while
        // EachRange has numeric bounds in the corresponding argument positions.
        val pattern = """{"field":"bidType","arguments":[{"name":"bidType"},[],{"pattern":"[a-z]*"}]}"""
        val range = """{"field":"year","arguments":[{"name":"year"},2200,1900]}"""
        val original = """{"errors":[$pattern,$range],"status":400}"""
        val changed = """{"errors":[$range,$pattern],"status":400}"""
        for (order in permutations(listOf(changed, original))) {
            val text = emit(format, original, order)
            assertFalse(active(text).any { it.contains("'errors'[0].'arguments'[1]") || it.contains("'errors'[1].'arguments'[1]") }, text)
            assertTrue(active(text).any { it.contains("'errors'.size()") }, text)
            assertTrue(active(text).any { it.contains("'status'") }, text)
            assertTrue(text.contains("// .body(\"'errors'[0].'arguments'[1].size()\""), text)
        }
    }
}
