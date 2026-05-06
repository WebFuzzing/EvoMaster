package org.evomaster.core.problem.asyncapi.broker

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Kafka backing for [MessageBrokerClient].
 *
 * Each instance pins its own consumer group id so test runs do not replay
 * each other's offsets.  Subscribed topics are managed in a single
 * [KafkaConsumer] (re-subscribe per channel as new ones are awaited) — this
 * keeps the bridge simple at the cost of having to drain the consumer in a
 * single thread, which is fine for the synchronous publish→reply flow the
 * AsyncAPI fitness layer uses.
 */
class KafkaBrokerClient(
    private val bootstrapServers: String,
    /** Per-run consumer group id; defaults to a unique one. */
    private val consumerGroupId: String = "evomaster-${UUID.randomUUID()}",
    private val consumerPollMs: Long = 200
) : MessageBrokerClient {

    companion object {
        private val log = LoggerFactory.getLogger(KafkaBrokerClient::class.java)
    }

    private var producer: KafkaProducer<String, ByteArray>? = null
    private var consumer: KafkaConsumer<String, ByteArray>? = null
    private val subscribedChannels = LinkedHashSet<String>()
    private var connected = false

    override fun connect() {
        if (connected) return

        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.CLIENT_ID_CONFIG, "evomaster-asyncapi-producer")
        }
        producer = KafkaProducer(producerProps)

        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java.name)
            // Start at the latest offset — we only care about messages produced
            // *after* this client connects.  EvoMaster always publishes before
            // awaiting, so latest is the right baseline.
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }
        consumer = KafkaConsumer(consumerProps)

        connected = true
    }

    override fun publish(
        channel: String,
        key: String?,
        headers: Map<String, ByteArray>,
        payload: ByteArray
    ): MessageBrokerClient.PublishOutcome {
        ensureConnected()
        val record = ProducerRecord<String, ByteArray>(channel, key, payload)
        headers.forEach { (k, v) -> record.headers().add(RecordHeader(k, v)) }
        return try {
            val metadata = producer!!.send(record).get(10, TimeUnit.SECONDS)
            MessageBrokerClient.PublishOutcome.Sent(
                mapOf(
                    "topic" to metadata.topic(),
                    "partition" to metadata.partition().toString(),
                    "offset" to metadata.offset().toString()
                )
            )
        } catch (e: Exception) {
            log.warn("Kafka publish to {} failed: {}", channel, e.message)
            MessageBrokerClient.PublishOutcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    override fun awaitFirstMatching(
        channel: String,
        predicate: (Map<String, ByteArray>) -> Boolean,
        timeoutMs: Long
    ): MessageBrokerClient.SubscribeOutcome {
        ensureConnected()
        ensureSubscribed(channel)

        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1)
            val poll = Duration.ofMillis(remaining.coerceAtMost(consumerPollMs))
            val records = consumer!!.poll(poll)
            for (rec in records) {
                if (rec.topic() != channel) continue
                val headers = rec.headers().associate { it.key() to it.value() }
                if (predicate(headers)) {
                    return MessageBrokerClient.SubscribeOutcome.Received(rec.value(), headers)
                }
            }
        }
        return MessageBrokerClient.SubscribeOutcome.Timeout
    }

    override fun close() {
        try {
            producer?.close(Duration.ofSeconds(2))
        } catch (e: Exception) {
            log.debug("KafkaProducer close failed: {}", e.message)
        }
        try {
            consumer?.close(Duration.ofSeconds(2))
        } catch (e: Exception) {
            log.debug("KafkaConsumer close failed: {}", e.message)
        }
        producer = null
        consumer = null
        subscribedChannels.clear()
        connected = false
    }

    private fun ensureConnected() {
        if (!connected) connect()
    }

    private fun ensureSubscribed(channel: String) {
        if (!subscribedChannels.add(channel)) return
        consumer!!.subscribe(subscribedChannels)
        // First poll bootstraps the assignment so the next poll returns
        // records actually published to this channel.
        consumer!!.poll(Duration.ofMillis(consumerPollMs))
    }
}
