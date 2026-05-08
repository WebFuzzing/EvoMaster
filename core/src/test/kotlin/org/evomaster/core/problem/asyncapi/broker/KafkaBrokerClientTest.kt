package org.evomaster.core.problem.asyncapi.broker

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Round-trips a Kafka publish/subscribe through [KafkaBrokerClient] against a
 * Testcontainers-managed broker.  Mirrors the existing core-side
 * Testcontainers tests (e.g. MySQLInsertValueTest) — runs under surefire and
 * is implicitly skipped when Docker isn't available.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaBrokerClientTest {

    private val kafka: KafkaContainer = KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    )

    @BeforeAll
    fun startKafka() {
        kafka.start()
    }

    @AfterAll
    fun stopKafka() {
        kafka.stop()
    }

    @Test
    fun publishedMessageIsObservableViaCorrelationFilter() {
        val client = KafkaBrokerClient(bootstrapServers = kafka.bootstrapServers)
        try {
            client.connect()

            val topic = "broker-roundtrip-${UUID.randomUUID()}"
            val correlationId = "evm-${UUID.randomUUID()}"
            val payload = """{"hello":"world"}""".toByteArray(StandardCharsets.UTF_8)

            // Subscribe **before** publishing so the consumer's auto-offset
            // reset (`latest`) doesn't make us miss the message.  Run the
            // publish from a separate thread.
            val publishThread = Thread {
                Thread.sleep(500)  // small grace so the consumer lands on the partition first
                val outcome = client.publish(
                    channel = topic,
                    key = null,
                    headers = mapOf("evm-correlation-id" to correlationId.toByteArray(StandardCharsets.UTF_8)),
                    payload = payload
                )
                assertTrue(outcome is MessageBrokerClient.PublishOutcome.Sent, "publish should succeed; was $outcome")
            }
            publishThread.start()

            val received = client.awaitFirstMatching(
                channel = topic,
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
            assertEquals(payload.toString(StandardCharsets.UTF_8), r.payload.toString(StandardCharsets.UTF_8))
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
        val client = KafkaBrokerClient(bootstrapServers = kafka.bootstrapServers)
        try {
            client.connect()
            val topic = "broker-timeout-${UUID.randomUUID()}"
            val outcome = client.awaitFirstMatching(
                channel = topic,
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
        val client = KafkaBrokerClient(bootstrapServers = kafka.bootstrapServers)
        try {
            client.connect()
            val topic = "broker-mismatch-${UUID.randomUUID()}"

            val producerThread = Thread {
                Thread.sleep(500)
                val res = client.publish(
                    channel = topic,
                    key = null,
                    headers = mapOf("evm-correlation-id" to "actual-id".toByteArray(StandardCharsets.UTF_8)),
                    payload = """{"x":1}""".toByteArray(StandardCharsets.UTF_8)
                )
                assertNotNull(res)
            }
            producerThread.start()

            val outcome = client.awaitFirstMatching(
                channel = topic,
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
}
