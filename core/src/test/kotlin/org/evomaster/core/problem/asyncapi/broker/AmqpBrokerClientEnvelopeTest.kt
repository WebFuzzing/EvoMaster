package org.evomaster.core.problem.asyncapi.broker

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Pure-unit tests for the MassTransit envelope marshalling used by
 * [AmqpBrokerClient]. Runs without RabbitMQ — wrap/unwrap is pure JSON,
 * the network and exchange topology are not exercised here.
 *
 * The Level-2 round-trip is in [AmqpBrokerClientRoundTripTest].
 */
class AmqpBrokerClientEnvelopeTest {

    private val mapper = ObjectMapper()

    private fun newClient(): AmqpBrokerClient =
        AmqpBrokerClient(bootstrapUri = "amqp://unused")

    @Test
    fun envelopeStrategyDefaultsToNone() {
        assertEquals(AmqpBrokerClient.EnvelopeStrategy.NONE, newClient().envelopeStrategy)
    }

    @Test
    fun fromContentTypeMapsMassTransit() {
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.MASSTRANSIT,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType("application/vnd.masstransit+json")
        )
    }

    @Test
    fun fromContentTypeIsCaseInsensitive() {
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.MASSTRANSIT,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType("Application/Vnd.MassTransit+JSON")
        )
    }

    @Test
    fun fromContentTypeAllowsParameterSuffix() {
        // Real-world headers sometimes carry parameters like `; charset=utf-8`.
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.MASSTRANSIT,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType(
                "application/vnd.masstransit+json; charset=utf-8"
            )
        )
    }

    @Test
    fun fromContentTypeFallsBackToNoneOnUnknown() {
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.NONE,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType("application/json")
        )
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.NONE,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType(null)
        )
        assertEquals(
            AmqpBrokerClient.EnvelopeStrategy.NONE,
            AmqpBrokerClient.EnvelopeStrategy.fromContentType("")
        )
    }

    @Test
    fun configureContentTypeUpdatesStrategy() {
        val client = newClient()
        client.configureContentType("application/vnd.masstransit+json")
        assertEquals(AmqpBrokerClient.EnvelopeStrategy.MASSTRANSIT, client.envelopeStrategy)
        client.configureContentType("application/json")
        assertEquals(AmqpBrokerClient.EnvelopeStrategy.NONE, client.envelopeStrategy)
    }

    @Test
    fun wrapEmitsMassTransitShape() {
        val client = newClient()
        val inner = """{"id":"abc","amount":42}""".toByteArray(StandardCharsets.UTF_8)
        val wrapped = client.wrapMassTransit("BookWorm.Contracts:UserCheckedOut", inner)
        val node = mapper.readTree(wrapped)

        // messageId is a generated UUID — assert presence + parseability, not value.
        val messageId = node.get("messageId")?.asText()
        assertNotNull(messageId, "messageId field missing")
        assertTrue(messageId!!.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")),
            "messageId should be a UUID, was $messageId")

        // messageType derives from the channel address with the urn:message: prefix.
        val mt = node.get("messageType")
        assertTrue(mt.isArray, "messageType should be a JSON array")
        assertEquals(1, mt.size())
        assertEquals("urn:message:BookWorm.Contracts:UserCheckedOut", mt[0].asText())

        // Inner message preserved as a nested object (not a string), matching
        // what MassTransit's deserializer expects.
        val message = node.get("message")
        assertTrue(message.isObject, "message should be a nested JSON object, was: $message")
        assertEquals("abc", message.get("id").asText())
        assertEquals(42, message.get("amount").asInt())
    }

    @Test
    fun wrapFallsBackToStringWhenInnerIsNotJson() {
        val client = newClient()
        val inner = "not-json bytes".toByteArray(StandardCharsets.UTF_8)
        val wrapped = client.wrapMassTransit("Some.Channel", inner)
        val node = mapper.readTree(wrapped)
        // Defensive: caller passed non-JSON; we emit `message` as a string
        // rather than throwing, so the broker call still goes through and
        // the engine can observe DELIVERY_OK instead of dying mid-evaluation.
        assertTrue(node.get("message").isTextual,
            "message should be a string fallback; was: ${node.get("message")}")
        assertEquals("not-json bytes", node.get("message").asText())
    }

    @Test
    fun wrapDifferentChannelsProduceDifferentMessageTypes() {
        val client = newClient()
        val one = mapper.readTree(client.wrapMassTransit("X.Y:Foo", """{}""".toByteArray()))
        val two = mapper.readTree(client.wrapMassTransit("X.Y:Bar", """{}""".toByteArray()))
        assertEquals("urn:message:X.Y:Foo", one.get("messageType")[0].asText())
        assertEquals("urn:message:X.Y:Bar", two.get("messageType")[0].asText())
    }

    @Test
    fun unwrapExtractsInnerObject() {
        val client = newClient()
        val envelope = """
            {
              "messageId":"6d0d1f7e-9d0f-46af-8a5e-9c2e6b9f8e3b",
              "messageType":["urn:message:Some:Type"],
              "message":{"id":"abc","amount":42}
            }
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val unwrapped = client.unwrapMassTransit(envelope)
        val node = mapper.readTree(unwrapped)
        assertEquals("abc", node.get("id").asText())
        assertEquals(42, node.get("amount").asInt())
        assertEquals(2, node.size(), "Only the inner fields should be present after unwrap")
    }

    @Test
    fun unwrapHandlesStringMessage() {
        val client = newClient()
        val envelope = """
            {
              "messageId":"u",
              "messageType":["urn:message:T"],
              "message":"hello"
            }
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val unwrapped = client.unwrapMassTransit(envelope)
        assertEquals("hello", String(unwrapped, StandardCharsets.UTF_8))
    }

    @Test
    fun unwrapFallsBackToOriginalOnNonEnvelope() {
        val client = newClient()
        // The SUT might publish a raw payload despite the document declaring
        // MassTransit content type — typical in misconfigured services.
        // Unwrap should return the bytes unchanged so the schema validator
        // still sees something.
        val raw = """{"foo":"bar"}""".toByteArray(StandardCharsets.UTF_8)
        val out = client.unwrapMassTransit(raw)
        assertEquals(String(raw, StandardCharsets.UTF_8), String(out, StandardCharsets.UTF_8))
    }

    @Test
    fun unwrapFallsBackOnInvalidJson() {
        val client = newClient()
        val raw = "garbage{".toByteArray(StandardCharsets.UTF_8)
        val out = client.unwrapMassTransit(raw)
        assertEquals(String(raw, StandardCharsets.UTF_8), String(out, StandardCharsets.UTF_8))
    }

    @Test
    fun wrapThenUnwrapRoundTripsThePayload() {
        val client = newClient()
        val inner = """{"a":1,"b":[true,false,null]}""".toByteArray(StandardCharsets.UTF_8)
        val wrapped = client.wrapMassTransit("Some.Channel", inner)
        val unwrapped = client.unwrapMassTransit(wrapped)
        // JSON equivalence — comparing serialised forms tolerates whitespace.
        assertEquals(mapper.readTree(inner), mapper.readTree(unwrapped))
    }
}
