package org.evomaster.core.output.service

import org.evomaster.core.EMConfig
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import java.time.Duration
import org.junit.jupiter.api.Test

/**
 * Checks the decisions made when writing a test (eg what is asserted on a reply, and what is
 * skipped), not the exact code written for them, which would break on any change of naming.
 */
class AsyncApiTestCaseWriterTest {

    private fun emit(payload: String?, configure: EMConfig.() -> Unit = {}): String {
        val config = EMConfig().apply {
            outputFormat = OutputFormat.KOTLIN_JUNIT_5
            configure()
        }
        val lines = Lines(config.outputFormat)
        AsyncApiTestCaseWriter(config).writeReplyAssertions(lines, payload, "res_0", "body_0")
        return lines.toString()
    }

    @Test
    fun testNothingIsWrittenWhenAssertionsAreOff() {
        assertEquals("", emit("""{"a": 1}""") { enableBasicAssertions = false }.trim())
    }

    @Test
    fun testAReplyLargerThanTheThresholdIsNotAssertedOn() {

        val emitted = emit("""{"x": "aaaaaaaaaaaaaaaaaaaaaaaaa"}""") { maxResponseByteSize = 10 }

        assertFalse(emitted.contains("assertTrue("), emitted)
        assertFalse(emitted.contains("assertEquals("), emitted)
        assertTrue(emitted.contains("larger than the threshold"), emitted)
    }

    @Test
    fun testAFieldToSkipIsNotAssertedOn() {

        val emitted = emit("""{"id": 7, "kept": 2}""")

        assertTrue(emitted.contains("id is not asserted on"), emitted)
        assertTrue(emitted.contains("kept"), emitted)
    }

    @Test
    fun testAConfiguredFieldToSkipIsNotAssertedOn() {

        val emitted = emit("""{"token": "abc"}""") { fieldsToSkipInAssertions = "token" }

        assertTrue(emitted.contains("token is not asserted on"), emitted)
        assertFalse(emitted.contains("abc"), emitted)
    }

    @Test
    fun testAValueNotSuitableToPrintIsNotAssertedOn() {

        val emitted = emit("""{"where": "localhost:12345"}""")

        assertFalse(emitted.contains("localhost:12345"), emitted)
        assertTrue(emitted.contains("where is not asserted on"), emitted)
    }

    @Test
    fun testANumberOutsideADoubleIsNotAssertedOn() {

        val emitted = emit("""{"x": 1e400}""")

        assertFalse(emitted.contains("Infinity"), emitted)
        assertTrue(emitted.contains("outside the range of a double"), emitted)
    }

    @Test
    fun testOnlyTheFirstElementsOfACollectionAreAsserted() {

        val emitted = emit("""{"xs": [1, 2, 3, 4]}""") { maxAssertionForDataInCollection = 2 }

        assertTrue(emitted.contains("get(1)"), emitted)
        assertFalse(emitted.contains("get(2)"), emitted)
        assertTrue(emitted.contains("the remaining 2 elements are not asserted on"), emitted)
    }

    @Test
    fun testANegativeCollectionLimitAssertsTheWholeCollection() {

        val emitted = emit("""{"xs": [1, 2, 3]}""") { maxAssertionForDataInCollection = -1 }

        assertTrue(emitted.contains("get(2)"), emitted)
        assertFalse(emitted.contains("not asserted on"), emitted)
    }

    @Test
    fun testANumberTooLargeForAnIntIsALongInJava() {

        val emitted = emit("""{"n": 5000000000}""") { outputFormat = OutputFormat.JAVA_JUNIT_5 }

        assertTrue(emitted.contains("5000000000L"), emitted)
    }

    @Test
    fun testAnUnsupportedFormatIsRejected() {

        assertThrows(IllegalArgumentException::class.java) {
            emit("""{"a": 1}""") { outputFormat = OutputFormat.PYTHON_UNITTEST }
        }
    }

    @Test
    fun testEachPublishedMessageIsGivenTheWholeSettleWindow() {
        val run = compiledSettleTest("EachMessage", 200, 2) {
            FakeAsyncApiDriver.fireAndForget().apply { settledMs = 50L }
        }
        val calls = run()
        assertEquals(3, calls.size)
        calls.zipWithNext().forEach { (before, after) ->
            assertTrue(after - before >= 200_000_000L, "Each message must get the configured window")
        }
    }

    @Test
    fun testMessagesWithRepliesOrTimeoutsAreGivenTheSettleWindow() {
        listOf(FakeAsyncApiDriver.replied(AsyncApiTestInjector.DOUBLE_RESULT), FakeAsyncApiDriver.silence())
            .forEachIndexed { index, reply ->
                val run = compiledSettleTest("Reply$index", 200) { reply.apply { settledMs = 50L } }
                val calls = run()
                assertEquals(2, calls.size)
                assertTrue(calls.last() - calls.first() >= 200_000_000L)
            }
    }

    @Test
    fun testNoSettleWaitIsWrittenWhenNoMessageWasKeptOpen() {
        val noWindow = compiledSettleTest("NoWindow", 0) { FakeAsyncApiDriver.fireAndForget() }
        val failed = compiledSettleTest("Failed", 60_000) {
            FakeAsyncApiDriver.fireAndForget().apply { published = false }
        }
        assertTimeoutPreemptively(Duration.ofSeconds(2)) {
            assertEquals(2, noWindow().size)
            assertEquals(1, failed().size)
        }
    }

    /** Compile the generated method and observe when its scripted actions execute. */
    private fun compiledSettleTest(
        name: String,
        settleMs: Int,
        count: Int = 1,
        answer: () -> AsyncApiReplyDto
    ): () -> List<Long> {
        val injector = AsyncApiTestInjector.create(
            AsyncApiTestInjector.ncsDriver { action ->
                answer().apply {
                    testScript = listOf("calls.add(System.nanoTime())", "val ${action.replyVariable} = \"reply\"")
                }
            },
            "--blackBox=false", "--createTests=true", "--enableBasicAssertions=false", "--testTimeout=0",
            "--outputFormat=KOTLIN_JUNIT_5", "--asyncApiSettleMs=$settleMs"
        )
        val evaluated = AsyncApiTestInjector.evaluate(injector, *Array(count) { "bessj" })
        val body = injector.getInstance(TestCaseWriter::class.java)
            .convertToCompilableTestCode(TestCase(evaluated, "testSettle"), "").toString()
        val className = "GeneratedSettle$name"
        CompilerForTestGenerated.compile(OutputFormat.KOTLIN_JUNIT_5, """
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.Assertions.*
            class $className {
                val calls = mutableListOf<Long>()
                $body
            }
        """.trimIndent(), className)
        val klass = javaClass.classLoader.loadClass(className)
        val instance = klass.getDeclaredConstructor().newInstance()
        return {
            klass.getDeclaredMethod("testSettle").invoke(instance)
            @Suppress("UNCHECKED_CAST")
            val calls = klass.getDeclaredMethod("getCalls").invoke(instance) as List<Long>
            calls + System.nanoTime()
        }
    }
}
