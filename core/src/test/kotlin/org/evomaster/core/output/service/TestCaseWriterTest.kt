package org.evomaster.core.output.service

import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestCase
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.nio.file.Path

class TestCaseWriterTest {

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_4", "JAVA_JUNIT_5", "KOTLIN_JUNIT_4", "KOTLIN_JUNIT_5"])
    fun testJUnitAssertionsAcceptJavaAndKotlin(format: OutputFormat) {
        val writer = AssertionWriter(format)
        for (assertion in AssertionKind.values()) {
            val lines = Lines(format)
            assertDoesNotThrow { writer.write(assertion, lines) }
            assertEquals(1, lines.nextLineNumber())
        }
    }

    @ParameterizedTest
    @EnumSource(OutputFormat::class, names = ["JAVA_JUNIT_4", "JAVA_JUNIT_5", "KOTLIN_JUNIT_4", "KOTLIN_JUNIT_5"], mode = EnumSource.Mode.EXCLUDE)
    fun testJUnitAssertionsRejectOtherFormatsWithoutWriting(format: OutputFormat) {
        val writer = AssertionWriter(format)
        for (assertion in AssertionKind.values()) {
            val lines = Lines(format)
            assertThrows(IllegalArgumentException::class.java) { writer.write(assertion, lines) }
            assertTrue(lines.isEmpty())
        }
    }

    private enum class AssertionKind { TRUE, EQUALS, EQUALS_WITH_DELTA, CONTAINS }

    /** Exposes the protected assertion helpers without involving a particular API writer. */
    private class AssertionWriter(outputFormat: OutputFormat) : TestCaseWriter() {
        init {
            config = EMConfig().apply { this.outputFormat = outputFormat }
        }

        fun write(assertion: AssertionKind, lines: Lines) {
            when (assertion) {
                AssertionKind.TRUE -> writeJUnitAssertTrue(lines, "true")
                AssertionKind.EQUALS -> writeJUnitAssertEquals(lines, "1", "1")
                AssertionKind.EQUALS_WITH_DELTA -> writeJUnitAssertEqualsWithDelta(lines, "1.0", "1.0", 0.001)
                AssertionKind.CONTAINS -> writeJUnitAssertContains(lines, "value", "a\"b")
            }
        }

        override fun addTestCommentBlock(lines: Lines, test: TestCase) {
            throw UnsupportedOperationException("Not used by assertion tests")
        }

        override fun handleTestInitialization(
            lines: Lines,
            baseUrlOfSut: String,
            ind: EvaluatedIndividual<*>,
            sqlInsertionVars: MutableList<Pair<String, String>>,
            mongoInsertionVars: MutableList<Pair<String, String>>,
            redisInsertionVars: MutableList<Pair<String, String>>,
            dynamoDbInsertionVars: MutableList<Pair<String, String>>,
            neo4jInsertionVars: MutableList<Pair<String, String>>,
            testName: String
        ) {
            throw UnsupportedOperationException("Not used by assertion tests")
        }

        override fun handleActionCalls(
            lines: Lines,
            baseUrlOfSut: String,
            ind: EvaluatedIndividual<*>,
            sqlInsertionVars: MutableList<Pair<String, String>>,
            mongoInsertionVars: MutableList<Pair<String, String>>,
            redisInsertionVars: MutableList<Pair<String, String>>,
            dynamoDbInsertionVars: MutableList<Pair<String, String>>,
            neo4jInsertionVars: MutableList<Pair<String, String>>,
            testCaseName: String,
            testSuitePath: Path?
        ) {
            throw UnsupportedOperationException("Not used by assertion tests")
        }

        override fun addActionLinesPerType(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {
            throw UnsupportedOperationException("Not used by assertion tests")
        }

        override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
            throw UnsupportedOperationException("Not used by assertion tests")
        }
    }
}
