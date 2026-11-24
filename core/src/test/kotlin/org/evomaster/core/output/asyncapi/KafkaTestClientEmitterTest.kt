package org.evomaster.core.output.asyncapi

import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Checks which recorded emissions can be observed; generated programs are exercised by E2E tests. */
class KafkaTestClientEmitterTest {

    @Test
    fun testOnlyDeclaredTopicsOtherThanThePublishedTopicAreObserved() {
        val result = result("input", "input", "output", "empty")
        val first = message("output", "first")
        val second = message("output", "second")
        result.setEmittedMessages(listOf(message("input", "request"), first, message("audit", "ignored"), second))

        val observed = KafkaTestClientEmitter.observedTopicsOf(result)
        assertEquals(setOf("output"), observed.keys)
        assertEquals(listOf("first", "second"), observed.getValue("output").map { it.payload })
    }

    @Test
    fun testNoObserverIsWrittenWithoutDeclaredEmissions() {
        val result = result("input", "output")
        result.setEmittedMessages(listOf(message("audit", "ignored")))
        listOf(OutputFormat.JAVA_JUNIT_5, OutputFormat.KOTLIN_JUNIT_5).forEach { format ->
            val lines = Lines(format)
            KafkaTestClientEmitter.emitObserve(lines, result, "observer", "broker", format)
            KafkaTestClientEmitter.emitAwait(lines, result, "observer", "messages", 100L, format)
            assertTrue(lines.isEmpty())
        }
    }

    @Test
    fun testUnsupportedObservationFormatsAreRejected() {
        val result = result("input", "output")
        result.setEmittedMessages(listOf(message("output", "answer")))
        val lines = Lines(OutputFormat.PYTHON_UNITTEST)
        assertThrows(IllegalArgumentException::class.java) {
            KafkaTestClientEmitter.emitObserve(lines, result, "observer", "broker", OutputFormat.PYTHON_UNITTEST)
        }
        assertThrows(IllegalArgumentException::class.java) {
            KafkaTestClientEmitter.emitAwait(lines, result, "observer", "messages", 100L, OutputFormat.PYTHON_UNITTEST)
        }
    }

    private fun message(topic: String, body: String) = EmittedMessageDto().apply {
        address = topic
        payload = body
        payloadType = String::class.java.name
    }

    private fun result(address: String, vararg declared: String) = AsyncApiCallResult("action").apply {
        setPublishDetails("kafka", "localhost:9092", "kafka", address, null, "{}", "{}", null, null)
        setDeclaredEmissionAddresses(declared.toList())
    }
}
