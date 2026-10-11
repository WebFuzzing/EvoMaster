package org.evomaster.core.output.asyncapi

import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * What observing the messages a service emits becomes, in each language a suite can be written
 * in. The Java and Kotlin forms are compiled by AsyncApiTestCaseWriterTest and run by the E2E
 * tests; the Python form is parsed here.
 */
class KafkaTestClientEmitterTest {

    companion object {
        private const val CONFIRMED = "shop.orders.confirmed"
        private const val STOCK = "shop.stock.levels"
        private const val UNDECLARED = "shop.audit"
        private const val PLACED = "shop.orders.placed"
        private const val ORDER = """{"orderId": "o-1"}"""
        private const val SERVER = "kafka"
        private const val BROKER_ADDRESS = "localhost:9092"
        private const val PROTOCOL = "kafka"
        private const val NO_HEADERS = "{}"
        private const val VARIABLE = "res_0"
        private const val CONFIRMATION = """{"orderId": "o-1", "status": "CONFIRMED"}"""
        private const val OBSERVER = "observer_0"
        private const val EMITTED = "emitted_0"
        private const val PAYLOAD_PREFIX = "emitted_0_"
        private const val BODY_PREFIX = "emittedBody_0_"
        private const val BROKER = "asyncApiServer_kafka"
        private const val TIMEOUT_MS = 5000L

        private val FORMATS = listOf(OutputFormat.JAVA_JUNIT_5, OutputFormat.KOTLIN_JUNIT_5, OutputFormat.PYTHON_UNITTEST)
    }

    private fun message(address: String, payload: String? = CONFIRMATION, truncated: Boolean = false, type: String? = null) =
        EmittedMessageDto().apply {
            this.address = address
            this.payload = payload
            payloadTruncated = truncated
            payloadType = type ?: String::class.java.name
        }

    /**
     * A result whose message made the service emit [messages], of which those to [declared] are
     * to addresses the contract declares for it to send to.
     */
    private fun result(declared: List<String>, vararg messages: EmittedMessageDto) =
        resultPublishedTo(PLACED, declared, *messages)

    /**
     * The same, for a message published to [address].
     */
    private fun resultPublishedTo(address: String, declared: List<String>, vararg messages: EmittedMessageDto) =
        AsyncApiCallResult("a0").apply {
            setPublished(SERVER, BROKER_ADDRESS, PROTOCOL, address, null, ORDER, NO_HEADERS, null, null)
            setEmittedMessages(messages.toList())
            if (declared.isNotEmpty()) {
                setDeclaredEmissionAddresses(declared)
            }
        }

    private fun lines(format: OutputFormat, write: (Lines) -> Unit): String =
        Lines(format).also(write).toString()

    private fun observe(result: AsyncApiCallResult, format: OutputFormat) =
        lines(format) { KafkaTestClientEmitter.emitObserve(it, result, OBSERVER, BROKER, format) }

    private fun publish(result: AsyncApiCallResult, format: OutputFormat, observer: String?) =
        lines(format) { KafkaTestClientEmitter.emit(it, result, VARIABLE, BROKER, format, observer) }

    private fun await(result: AsyncApiCallResult, format: OutputFormat) =
        lines(format) { KafkaTestClientEmitter.emitAwait(it, result, OBSERVER, EMITTED, TIMEOUT_MS, format) }

    private fun assertions(result: AsyncApiCallResult, format: OutputFormat, config: EMConfig = EMConfig()) =
        lines(format) {
            KafkaTestClientEmitter.emitEmittedAssertions(it, result, EMITTED, PAYLOAD_PREFIX, BODY_PREFIX, config, format)
        }

    @Test
    fun testOnlyTheDeclaredAddressesAreObserved() {

        val result = result(listOf(CONFIRMED, STOCK), message(CONFIRMED), message(UNDECLARED), message(STOCK), message(CONFIRMED))

        val topics = KafkaTestClientEmitter.observedTopicsOf(result)

        //in the order first emitted to, each with what went there in order
        assertEquals(listOf(CONFIRMED, STOCK), topics.keys.toList())
        assertEquals(2, topics.getValue(CONFIRMED).size)
        assertEquals(1, topics.getValue(STOCK).size)
    }

    @Test
    fun testNothingIsWrittenWhenNothingDeclaredWasEmitted() {

        //what the service emitted, but nowhere it is declared to send to
        val result = result(listOf(), message(UNDECLARED))

        assertTrue(KafkaTestClientEmitter.observedTopicsOf(result).isEmpty())
        FORMATS.forEach { format ->
            assertEquals("", observe(result, format).trim(), "for $format")
            assertEquals("", await(result, format).trim(), "for $format")
            assertEquals("", assertions(result, format).trim(), "for $format")
        }
    }

    @Test
    fun testTheObserverIsStartedAndCollectedInTheLanguageOfTheSuite() {

        val result = result(listOf(CONFIRMED, STOCK), message(CONFIRMED), message(STOCK), message(STOCK))

        assertEquals(
            "AutoCloseable $OBSERVER = observeEmitted($BROKER, \"$CONFIRMED\", \"$STOCK\");",
            observe(result, OutputFormat.JAVA_JUNIT_5).trim()
        )
        assertEquals(
            "val $OBSERVER = observeEmitted($BROKER, \"$CONFIRMED\", \"$STOCK\")",
            observe(result, OutputFormat.KOTLIN_JUNIT_5).trim()
        )
        assertEquals(
            "$OBSERVER = self.observeEmitted($BROKER, \"$CONFIRMED\", \"$STOCK\")",
            observe(result, OutputFormat.PYTHON_UNITTEST).trim()
        )

        //one expected on the first topic and two on the second, so neither ends the wait for the other
        assertEquals(
            "java.util.Map<String, java.util.List<String>> $EMITTED = awaitEmitted($OBSERVER, new int[]{1, 2}, 5000L, \"$CONFIRMED\", \"$STOCK\");",
            await(result, OutputFormat.JAVA_JUNIT_5).trim()
        )
        assertEquals(
            "val $EMITTED = awaitEmitted($OBSERVER, intArrayOf(1, 2), 5000L, \"$CONFIRMED\", \"$STOCK\")",
            await(result, OutputFormat.KOTLIN_JUNIT_5).trim()
        )
        assertEquals(
            "$EMITTED = self.awaitEmitted($OBSERVER, [1, 2], 5000, \"$CONFIRMED\", \"$STOCK\")",
            await(result, OutputFormat.PYTHON_UNITTEST).trim()
        )
    }

    @Test
    fun testATopicThatGotOneMessageHasItsPayloadAssertedOn() {

        val result = result(listOf(CONFIRMED), message(CONFIRMED))

        val java = assertions(result, OutputFormat.JAVA_JUNIT_5)
        assertTrue(java.contains("assertTrue($EMITTED.get(\"$CONFIRMED\").size() >= 1);"), java)
        //only when it is all that arrived there
        assertTrue(java.contains("if ($EMITTED.get(\"$CONFIRMED\").size() == 1) {"), java)
        assertTrue(java.contains("String ${PAYLOAD_PREFIX}0 = $EMITTED.get(\"$CONFIRMED\").get(0);"), java)
        assertTrue(java.contains("assertEquals(\"CONFIRMED\", ${BODY_PREFIX}0.get(\"status\").asText());"), java)

        val kotlin = assertions(result, OutputFormat.KOTLIN_JUNIT_5)
        assertTrue(kotlin.contains("assertTrue($EMITTED.getValue(\"$CONFIRMED\").size >= 1)"), kotlin)
        assertTrue(kotlin.contains("if ($EMITTED.getValue(\"$CONFIRMED\").size == 1) {"), kotlin)
        assertTrue(kotlin.contains("val ${PAYLOAD_PREFIX}0 = $EMITTED.getValue(\"$CONFIRMED\")[0]"), kotlin)
        assertTrue(kotlin.contains("assertTrue(${BODY_PREFIX}0.has(\"orderId\"))"), kotlin)

        val python = assertions(result, OutputFormat.PYTHON_UNITTEST)
        assertTrue(python.contains("assert len($EMITTED[\"$CONFIRMED\"]) >= 1"), python)
        assertTrue(python.contains("if len($EMITTED[\"$CONFIRMED\"]) == 1:"), python)
        assertTrue(python.contains("${PAYLOAD_PREFIX}0 = $EMITTED[\"$CONFIRMED\"][0]"), python)
        assertTrue(python.contains("${BODY_PREFIX}0 = json.loads(${PAYLOAD_PREFIX}0)"), python)
    }

    @Test
    fun testSeveralMessagesOnATopicHaveOnlyTheirCountAssertedOn() {

        val result = result(listOf(CONFIRMED), message(CONFIRMED), message(CONFIRMED))

        val java = assertions(result, OutputFormat.JAVA_JUNIT_5)

        assertTrue(java.contains("assertTrue($EMITTED.get(\"$CONFIRMED\").size() >= 2);"), java)
        assertTrue(java.contains("may arrive in any order"), java)
        assertFalse(java.contains(PAYLOAD_PREFIX), java)
    }

    @Test
    fun testAPayloadThatWasNotReadIsNotAssertedOnAndTheTestSaysWhy() {

        val objects = result(listOf(CONFIRMED), message(CONFIRMED, payload = null, type = "com.foo.OrderConfirmed"))
        val java = assertions(objects, OutputFormat.JAVA_JUNIT_5)
        assertTrue(java.contains("size() >= 1"), java)
        assertTrue(java.contains("com.foo.OrderConfirmed"), java)
        assertFalse(java.contains(PAYLOAD_PREFIX), java)

        val cut = result(listOf(CONFIRMED), message(CONFIRMED, truncated = true))
        val kotlin = assertions(cut, OutputFormat.KOTLIN_JUNIT_5)
        assertTrue(kotlin.contains("only the start"), kotlin)
        assertFalse(kotlin.contains(PAYLOAD_PREFIX), kotlin)
    }

    @Test
    fun testANullValueIsNotMistakenForAnObjectTheClientSerialized() {

        val tombstone = result(listOf(CONFIRMED), message(CONFIRMED, payload = null).apply { payloadType = null })

        FORMATS.forEach { format ->
            val written = assertions(tombstone, format)
            assertTrue(written.contains("null value"), "for $format:\n$written")
            assertFalse(written.contains("turned into bytes"), "for $format:\n$written")
            assertFalse(written.contains(PAYLOAD_PREFIX), "for $format:\n$written")
        }
    }

    @Test
    fun testNoPayloadBlockIsWrittenWhenNothingWouldBeAssertedInIt() {

        val result = result(listOf(CONFIRMED), message(CONFIRMED))

        //assertions turned off: only the count, which is what the wait is for
        val off = EMConfig().apply { enableBasicAssertions = false }
        FORMATS.forEach { format ->
            val written = assertions(result, format, off)
            assertTrue(written.contains(">= 1"), "for $format:\n$written")
            assertFalse(written.contains(PAYLOAD_PREFIX), "for $format:\n$written")
            assertFalse(written.contains("arrived alone"), "for $format:\n$written")
        }

        //too large to assert on: the reason, without a block around it
        val small = EMConfig().apply { maxResponseByteSize = 4 }
        FORMATS.forEach { format ->
            val written = assertions(result, format, small)
            assertTrue(written.contains("threshold"), "for $format:\n$written")
            assertFalse(written.contains(PAYLOAD_PREFIX), "for $format:\n$written")
        }
    }

    @Test
    fun testAPayloadASerializerTurnsIntoBytesIsNotAssertedOnEvenWhenItsTextWasRecorded() {

        //what String.valueOf gives for a Long, which a LongSerializer writes as eight bytes
        val boxed = result(listOf(CONFIRMED), message(CONFIRMED, payload = "42", type = java.lang.Long::class.java.name))

        FORMATS.forEach { format ->
            val written = assertions(boxed, format)
            assertTrue(written.contains(java.lang.Long::class.java.name), "for $format:\n$written")
            assertFalse(written.contains(PAYLOAD_PREFIX), "for $format:\n$written")
        }

        //bytes are recorded decoded as UTF-8, which is how the test's consumer reads them too
        val bytes = result(listOf(CONFIRMED), message(CONFIRMED, type = ByteArray::class.java.name))
        val java = assertions(bytes, OutputFormat.JAVA_JUNIT_5)
        assertTrue(java.contains("String ${PAYLOAD_PREFIX}0 = "), java)
    }

    @Test
    fun testTheAddressTheMessageIsPublishedToIsNotObserved() {

        //a service declared to send to the address it also receives from, as a relay would be
        val result = resultPublishedTo(CONFIRMED, listOf(CONFIRMED, STOCK), message(CONFIRMED), message(STOCK))

        assertEquals(listOf(STOCK), KafkaTestClientEmitter.observedTopicsOf(result).keys.toList())

        val relayOnly = resultPublishedTo(CONFIRMED, listOf(CONFIRMED), message(CONFIRMED))
        assertTrue(KafkaTestClientEmitter.observedTopicsOf(relayOnly).isEmpty())
        FORMATS.forEach { format ->
            assertEquals("", observe(relayOnly, format).trim(), "for $format")
        }
    }

    @Test
    fun testAFailedPublishClosesTheObserverInEveryLanguage() {

        val result = result(listOf(CONFIRMED), message(CONFIRMED))

        val java = publish(result, OutputFormat.JAVA_JUNIT_5, OBSERVER)
        assertTrue(java.contains("String $VARIABLE;"), java)
        assertTrue(java.contains("$VARIABLE = publishAndAwaitReply("), java)
        assertTrue(java.contains("} catch (Throwable failure) {"), java)
        assertTrue(java.contains("$OBSERVER.close();"), java)
        assertTrue(java.contains("throw failure;"), java)

        val kotlin = publish(result, OutputFormat.KOTLIN_JUNIT_5, OBSERVER)
        assertTrue(kotlin.contains("val $VARIABLE = try {"), kotlin)
        assertTrue(kotlin.contains("$OBSERVER.close()"), kotlin)

        val python = publish(result, OutputFormat.PYTHON_UNITTEST, OBSERVER)
        assertTrue(python.contains("except BaseException:"), python)
        assertTrue(python.contains("$OBSERVER.close()"), python)
        assertTrue(python.contains("raise"), python)

        //with nothing observed there is nothing to close, and the call stays one line
        FORMATS.forEach { format ->
            val plain = publish(result(listOf(), message(UNDECLARED)), format, OBSERVER)
            assertFalse(plain.contains("close()"), "for $format:\n$plain")
            assertEquals(1, plain.trim().lines().size, "for $format:\n$plain")
        }
    }

    @Test
    fun testEachObservedTopicGetsVariablesOfItsOwn() {

        val result = result(listOf(CONFIRMED, STOCK), message(CONFIRMED), message(STOCK, payload = """{"level": 3}"""))

        val kotlin = assertions(result, OutputFormat.KOTLIN_JUNIT_5)

        assertTrue(kotlin.contains("val ${PAYLOAD_PREFIX}0 = $EMITTED.getValue(\"$CONFIRMED\")[0]"), kotlin)
        assertTrue(kotlin.contains("val ${PAYLOAD_PREFIX}1 = $EMITTED.getValue(\"$STOCK\")[0]"), kotlin)
        assertTrue(kotlin.contains("assertEquals(3, ${BODY_PREFIX}1.get(\"level\").asLong())"), kotlin)
    }

    @Test
    fun testTheObserveHelpersAreWrittenInEveryLanguage() {

        val java = lines(OutputFormat.JAVA_JUNIT_5) { KafkaTestClientEmitter.emitObserveHelpers(it, OutputFormat.JAVA_JUNIT_5) }
        assertTrue(java.contains("private static AutoCloseable observeEmitted(String broker, String... topics)"), java)
        assertTrue(java.contains("awaitEmitted(AutoCloseable observer,"), java)
        //from the end, made to take effect before anything is published
        assertTrue(java.contains("consumer.seekToEnd(parts);"), java)
        assertTrue(java.contains("consumer.position(p);"), java)
        //a topic created by what was emitted is read from its start
        assertTrue(java.contains("consumer.seekToBeginning(created);"), java)
        assertTrue(java.contains("consumer.close();"), java)

        val kotlin = lines(OutputFormat.KOTLIN_JUNIT_5) { KafkaTestClientEmitter.emitObserveHelpers(it, OutputFormat.KOTLIN_JUNIT_5) }
        assertTrue(kotlin.contains("private fun observeEmitted(broker: String, vararg topics: String): AutoCloseable"), kotlin)
        assertTrue(kotlin.contains("consumer.seekToBeginning(created)"), kotlin)

        val python = lines(OutputFormat.PYTHON_UNITTEST) { KafkaTestClientEmitter.emitObserveHelpers(it, OutputFormat.PYTHON_UNITTEST) }
        assertTrue(python.contains("def observeEmitted(broker, *topics):"), python)
        assertTrue(python.contains("observer.seek_to_beginning(*created)"), python)
    }

    @Test
    fun testThePythonFormIsValidPython() {

        /*
            Indentation is part of the language, so helpers written as text can break it in a way
            no JVM format can. The helpers and the lines a test calls them with, together.
         */
        val format = OutputFormat.PYTHON_UNITTEST
        val result = result(listOf(CONFIRMED), message(CONFIRMED))

        val helpers = lines(format) { KafkaTestClientEmitter.emitObserveHelpers(it, format) }
        val body = observe(result, format) + publish(result, format, OBSERVER) + await(result, format) +
                assertions(result, format)

        val module = buildString {
            append("import json\nimport time\nimport kafka\n\n")
            append("class Suite:\n")
            helpers.lines().forEach { append("    ").append(it).append("\n") }
            append("    def test_0(self):\n")
            append("        $BROKER = 'localhost:9092'\n")
            body.lines().forEach { append("        ").append(it).append("\n") }
        }

        parsesAsPython(module)
    }

    private fun parsesAsPython(module: String) {

        val file = java.io.File.createTempFile("asyncapi_emitted", ".py")
        file.deleteOnExit()
        file.writeText(module)

        try {
            val process = try {
                ProcessBuilder("python3", "-m", "py_compile", file.absolutePath).redirectErrorStream(true).start()
            } catch (e: java.io.IOException) {
                Assumptions.assumeTrue(false, "python3 is not on the PATH")
                return
            }
            val output = process.inputStream.bufferedReader().readText()
            assertEquals(0, process.waitFor(), "the generated Python does not parse:\n$output\n\n$module")
        } finally {
            file.delete()
        }
    }
}
