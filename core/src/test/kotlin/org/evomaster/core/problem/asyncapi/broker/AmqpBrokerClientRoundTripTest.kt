package org.evomaster.core.problem.asyncapi.broker

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Round-trips an AMQP publish / subscribe through [AmqpBrokerClient] against
 * a Testcontainers-managed RabbitMQ broker. Mirrors the existing
 * [KafkaBrokerClientTest] integration shape — runs under surefire and is
 * implicitly skipped when Docker isn't available (Testcontainers detects
 * the missing daemon and aborts the test class with a clear error).
 *
 * Pinned to RabbitMQ 3.12 because the 3.13 image hits a known
 * `.erlang.cookie: eacces` startup race on Docker Desktop's overlay-fs
 * (see `asyncapi-validation/shared/lib.sh` for the same workaround in
 * the validation harness).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmqpBrokerClientRoundTripTest {

    private val rabbit: RabbitMQContainer = RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.12-management")
    )

    @BeforeAll
    fun startRabbit() {
        rabbit.start()
    }

    @AfterAll
    fun stopRabbit() {
        rabbit.stop()
    }

    /**
     * Build the bootstrap URI in the same shape that the validation
     * harness uses, including the `%2F` escape for the default vhost
     * `/`. The Testcontainers container reports `amqp://host:port` (no
     * vhost path); we append the encoded default vhost so the broker
     * doesn't reject the connection with `vhost ' ' not found`.
     */
    private fun bootstrapUri(): String =
        "${rabbit.amqpUrl}/%2F"

    @Test
    fun publishedMessageIsObservableViaCorrelationFilter() {
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        try {
            client.connect()

            val channel = "broker-roundtrip-${UUID.randomUUID()}"
            val correlationId = "evm-${UUID.randomUUID()}"
            val payload = """{"hello":"world"}""".toByteArray(StandardCharsets.UTF_8)

            // Subscribe before publishing so the consumer queue is bound and
            // ready when the publish lands. Run the publish from a separate
            // thread so awaitFirstMatching can block on it.
            val publishThread = Thread {
                Thread.sleep(500)
                val outcome = client.publish(
                    channel = channel,
                    key = null,
                    headers = mapOf(
                        "evm-correlation-id" to correlationId.toByteArray(StandardCharsets.UTF_8)
                    ),
                    payload = payload
                )
                assertTrue(
                    outcome is MessageBrokerClient.PublishOutcome.Sent,
                    "publish should succeed; was $outcome"
                )
            }
            publishThread.start()

            val received = client.awaitFirstMatching(
                channel = channel,
                predicate = { headers ->
                    headers["evm-correlation-id"]?.toString(StandardCharsets.UTF_8) == correlationId
                },
                timeoutMs = 15_000
            )

            publishThread.join(5_000)
            assertTrue(
                received is MessageBrokerClient.SubscribeOutcome.Received,
                "expected Received, got $received"
            )
            val r = received as MessageBrokerClient.SubscribeOutcome.Received
            assertEquals(
                payload.toString(StandardCharsets.UTF_8),
                r.payload.toString(StandardCharsets.UTF_8)
            )
            assertEquals(
                correlationId,
                r.headers["evm-correlation-id"]?.toString(StandardCharsets.UTF_8)
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun subscribeReturnsTimeoutWhenNoMessageArrives() {
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        try {
            client.connect()
            val channel = "broker-timeout-${UUID.randomUUID()}"
            val outcome = client.awaitFirstMatching(
                channel = channel,
                predicate = { true },
                timeoutMs = 1_500
            )
            assertEquals(MessageBrokerClient.SubscribeOutcome.Timeout, outcome)
        } finally {
            client.close()
        }
    }

    @Test
    fun publishOnceConsumeRejectsWrongCorrelation() {
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        try {
            client.connect()
            val channel = "broker-mismatch-${UUID.randomUUID()}"

            val producerThread = Thread {
                Thread.sleep(500)
                val res = client.publish(
                    channel = channel,
                    key = null,
                    headers = mapOf(
                        "evm-correlation-id" to "actual-id".toByteArray(StandardCharsets.UTF_8)
                    ),
                    payload = """{"x":1}""".toByteArray(StandardCharsets.UTF_8)
                )
                assertNotNull(res)
            }
            producerThread.start()

            val outcome = client.awaitFirstMatching(
                channel = channel,
                predicate = { headers ->
                    headers["evm-correlation-id"]?.toString(StandardCharsets.UTF_8) == "different-id"
                },
                timeoutMs = 4_000
            )
            producerThread.join(5_000)
            assertEquals(MessageBrokerClient.SubscribeOutcome.Timeout, outcome)
        } finally {
            client.close()
        }
    }

    @Test
    fun collectAllWithinReturnsEveryPublishedMessage() {
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        try {
            client.connect()
            val channel = "broker-collect-${UUID.randomUUID()}"

            // Pre-bind the consumer queue by issuing one short collectAllWithin
            // call before the publishes (declares + binds the queue).
            client.collectAllWithin(channel, windowMs = 200)

            // Publish three distinct messages.
            val messages = listOf("a", "b", "c")
            messages.forEach { tag ->
                client.publish(
                    channel = channel,
                    key = null,
                    headers = mapOf("tag" to tag.toByteArray(StandardCharsets.UTF_8)),
                    payload = """{"msg":"$tag"}""".toByteArray(StandardCharsets.UTF_8)
                )
            }

            // Collect within a 2-second window — should see all three.
            val collected = client.collectAllWithin(channel, windowMs = 2_000)
            val tags = collected.mapNotNull {
                it.headers["tag"]?.toString(StandardCharsets.UTF_8)
            }.sorted()
            assertEquals(listOf("a", "b", "c"), tags)
        } finally {
            client.close()
        }
    }

    @Test
    fun masstransitModePublishAndConsumeRoundTrip() {
        // Mirrors a bookworm-style trip: declare a per-message-type fanout
        // exchange (the AsyncAPI channel address), publish a MassTransit
        // envelope, consume it via the same client, observe the unwrapped
        // inner payload. Exercises both halves of [EnvelopeStrategy.MASSTRANSIT]:
        // the publish-side wrap + channel-as-exchange topology, and the
        // receive-side queue-bind + envelope unwrap.
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        client.configureContentType("application/vnd.masstransit+json")
        try {
            client.connect()

            val channel = "BookWorm.Test:RoundTripEvent-${UUID.randomUUID()}"
            val innerJson = """{"orderId":"abc","total":42.5}"""
            val innerBytes = innerJson.toByteArray(StandardCharsets.UTF_8)

            val publishThread = Thread {
                Thread.sleep(500)
                val outcome = client.publish(
                    channel = channel,
                    key = null,
                    headers = emptyMap(),
                    payload = innerBytes
                )
                assertTrue(
                    outcome is MessageBrokerClient.PublishOutcome.Sent,
                    "publish should succeed; was $outcome"
                )
                val sent = outcome as MessageBrokerClient.PublishOutcome.Sent
                assertEquals("MASSTRANSIT", sent.protocolMetadata["envelope"])
                assertEquals(channel, sent.protocolMetadata["exchange"])
                assertEquals("", sent.protocolMetadata["routingKey"])
            }
            publishThread.start()

            val received = client.awaitFirstMatching(
                channel = channel,
                predicate = { true },
                timeoutMs = 15_000
            )
            publishThread.join(5_000)
            assertTrue(
                received is MessageBrokerClient.SubscribeOutcome.Received,
                "expected Received, got $received"
            )
            val r = received as MessageBrokerClient.SubscribeOutcome.Received
            // The consumer side unwraps; the caller sees the inner payload,
            // not the envelope. Comparing parsed JSON tolerates Jackson's
            // whitespace and key-order normalisation.
            val expected = ObjectMapper().readTree(innerJson)
            val actual = ObjectMapper().readTree(r.payload)
            assertEquals(expected, actual)
        } finally {
            client.close()
        }
    }

    @Test
    fun masstransitModeRejectsCrossExchangeContamination() {
        // Different channels = different per-message-type exchanges. Publishing
        // to exchange A should NOT cause a queue bound to exchange B to fire.
        // This pins the "channel address is the exchange name" semantic so a
        // future refactor that accidentally re-uses one exchange across
        // channels surfaces immediately.
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        client.configureContentType("application/vnd.masstransit+json")
        try {
            client.connect()

            val channelA = "BookWorm.Test:A-${UUID.randomUUID()}"
            val channelB = "BookWorm.Test:B-${UUID.randomUUID()}"

            // Pre-bind a consumer queue on channel B so the exchange-and-queue
            // exist by the time we publish on channel A.
            client.collectAllWithin(channelB, windowMs = 200)

            // Publish on channel A.
            client.publish(
                channel = channelA,
                key = null,
                headers = emptyMap(),
                payload = """{"v":1}""".toByteArray(StandardCharsets.UTF_8)
            )

            // B's queue should still be empty.
            val outcome = client.awaitFirstMatching(
                channel = channelB,
                predicate = { true },
                timeoutMs = 1_500
            )
            assertEquals(MessageBrokerClient.SubscribeOutcome.Timeout, outcome)
        } finally {
            client.close()
        }
    }

    @Test
    fun publishUtf8HeaderValuesRoundTripIntact() {
        val client = AmqpBrokerClient(bootstrapUri = bootstrapUri())
        try {
            client.connect()
            val channel = "broker-utf8-${UUID.randomUUID()}"
            val tenant = "café-équipe-€"  // non-ASCII to confirm UTF-8 wire encoding

            val publishThread = Thread {
                Thread.sleep(500)
                client.publish(
                    channel = channel,
                    key = null,
                    headers = mapOf("x-tenant" to tenant.toByteArray(StandardCharsets.UTF_8)),
                    payload = "{}".toByteArray(StandardCharsets.UTF_8)
                )
            }
            publishThread.start()

            val outcome = client.awaitFirstMatching(
                channel = channel,
                predicate = { headers ->
                    headers["x-tenant"]?.toString(StandardCharsets.UTF_8) == tenant
                },
                timeoutMs = 10_000
            )
            publishThread.join(5_000)
            assertTrue(
                outcome is MessageBrokerClient.SubscribeOutcome.Received,
                "expected Received, got $outcome"
            )
        } finally {
            client.close()
        }
    }
}
