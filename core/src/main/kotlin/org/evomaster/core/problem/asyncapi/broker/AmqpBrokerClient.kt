package org.evomaster.core.problem.asyncapi.broker

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * AMQP 0-9-1 (RabbitMQ) backing for [MessageBrokerClient].
 *
 * The AsyncAPI binding model is mapped to the simplest AMQP shape: each
 * channel address is treated as a routing key on the *default exchange*
 * (`""`). Under default-exchange semantics, a publish with routing key
 * `R` lands in a queue named `R` (if one exists); a subscribe declares
 * an auto-delete server-named queue and binds it to `R` so the engine
 * receives copies of anything published with that key. This avoids
 * having to mirror the SUT's exchange/queue topology — the schema
 * doesn't necessarily declare it, and the SUT is expected to have
 * created its own consumer queues.
 *
 * The richer AsyncAPI `amqp` binding (named exchanges, type, durability,
 * explicit queue bindings) is read from the schema in a follow-up PR;
 * the starter slice handles the bookworm-family SUTs in the validation
 * corpus, all of which use the default-exchange-as-queue pattern.
 *
 * `bootstrapUri` accepts either a full `amqp://user:pass@host:port/vhost`
 * URI or a bare `host:port` (treated as `amqp://host:port`).
 */
class AmqpBrokerClient(
    private val bootstrapUri: String,
    private val connectionFactory: ConnectionFactory = ConnectionFactory()
) : MessageBrokerClient {

    companion object {
        private val log = LoggerFactory.getLogger(AmqpBrokerClient::class.java)

        /**
         * Resolve [bootstrapUri] into a `URI` the RabbitMQ client accepts.
         * Bare `host:port` is normalised to `amqp://host:port`. Visible
         * for tests so the URI-parse contract can be unit-tested without
         * a broker.
         */
        fun resolveUri(bootstrapUri: String): URI {
            val trimmed = bootstrapUri.trim()
            return if (trimmed.startsWith("amqp://") || trimmed.startsWith("amqps://")) {
                URI.create(trimmed)
            } else {
                URI.create("amqp://$trimmed")
            }
        }
    }

    private var connection: Connection? = null
    private var channel: Channel? = null
    /**
     * One consumer queue per AsyncAPI channel. The queue is server-named
     * + auto-delete + exclusive — it lives as long as the connection
     * does and disappears on shutdown.
     */
    private data class Subscription(val queueName: String, val incoming: ConcurrentLinkedQueue<ReceivedDelivery>)
    private data class ReceivedDelivery(val payload: ByteArray, val headers: Map<String, ByteArray>)
    private val subscriptions = HashMap<String, Subscription>()
    private var closed = false

    override fun connect() {
        if (connection != null) return
        val uri = resolveUri(bootstrapUri)
        connectionFactory.setUri(uri)
        connection = connectionFactory.newConnection("evomaster-asyncapi")
        channel = connection!!.createChannel()
    }

    override fun publish(
        channel: String,
        key: String?,
        headers: Map<String, ByteArray>,
        payload: ByteArray
    ): MessageBrokerClient.PublishOutcome {
        if (closed) return MessageBrokerClient.PublishOutcome.Failed("client closed")
        return try {
            ensureConnected()
            // AMQP basicPublish takes BasicProperties carrying headers
            // and (optionally) a correlation id / content type / etc.
            // The MessageBrokerClient surface only gives us raw header
            // bytes, so we pack them into the message header table as
            // String values (UTF-8). Servers that read a specific
            // header (e.g. evm-correlation-id) get the same map as
            // for Kafka.
            val headerTable = headers.mapValues { (_, v) -> String(v, StandardCharsets.UTF_8) }
            val props = AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(headerTable)
                .build()
            // Default exchange ("") + channel as routing key.
            this.channel!!.basicPublish("", channel, props, payload)
            MessageBrokerClient.PublishOutcome.Sent(
                mapOf("routingKey" to channel, "bytes" to payload.size.toString())
            )
        } catch (e: Exception) {
            log.warn("AMQP publish to {} failed: {}", channel, e.message)
            MessageBrokerClient.PublishOutcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    override fun awaitFirstMatching(
        channel: String,
        predicate: (Map<String, ByteArray>) -> Boolean,
        timeoutMs: Long
    ): MessageBrokerClient.SubscribeOutcome {
        if (closed) return MessageBrokerClient.SubscribeOutcome.Timeout
        ensureConnected()
        val sub = ensureSubscribed(channel)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val d = drainOne(sub, deadline) ?: return MessageBrokerClient.SubscribeOutcome.Timeout
            if (predicate(d.headers)) {
                return MessageBrokerClient.SubscribeOutcome.Received(d.payload, d.headers)
            }
        }
        return MessageBrokerClient.SubscribeOutcome.Timeout
    }

    override fun collectAllWithin(
        channel: String,
        windowMs: Long
    ): List<MessageBrokerClient.ReceivedMessage> {
        if (closed) return emptyList()
        ensureConnected()
        val sub = ensureSubscribed(channel)
        val collected = mutableListOf<MessageBrokerClient.ReceivedMessage>()
        val deadline = System.currentTimeMillis() + windowMs
        while (System.currentTimeMillis() < deadline) {
            val d = drainOne(sub, deadline) ?: break
            collected.add(MessageBrokerClient.ReceivedMessage(d.payload, d.headers))
        }
        return collected
    }

    override fun close() {
        closed = true
        try { channel?.close() } catch (e: Exception) { log.debug("AMQP channel close: {}", e.message) }
        try { connection?.close(2_000) } catch (e: Exception) { log.debug("AMQP connection close: {}", e.message) }
        channel = null
        connection = null
        subscriptions.clear()
    }

    private fun ensureConnected() {
        if (connection == null) connect()
    }

    @Synchronized
    private fun ensureSubscribed(routingKey: String): Subscription {
        subscriptions[routingKey]?.let { return it }
        val ch = channel!!
        // queueDeclare("", false, true, true, null) → server-named,
        // non-durable, exclusive, auto-delete. Lives as long as the
        // connection. Bound to the default exchange by the same name,
        // so messages published with routingKey=<queueName> land here.
        val declared = ch.queueDeclare()
        val serverName = declared.queue
        // Bind our server-named queue to the routing key the engine
        // uses. The default exchange auto-binds by queue name, so we
        // also publish on a queue named after the routing key — to
        // receive copies of *both* what the SUT publishes (which goes
        // to a queue named after the same routing key, picked up by
        // our own queue if we set up a fan-out) AND what we publish
        // ourselves (so the test can self-observe). The starter slice
        // accepts that this only catches messages the SUT publishes
        // to the *default* exchange; named-exchange routing is a
        // follow-up.
        // For now: declare the routing-key queue (idempotent) and
        // consume from it directly. This matches the bookworm SUTs'
        // convention of using the default exchange.
        try {
            ch.queueDeclare(routingKey, false, false, true, null)
        } catch (e: Exception) {
            // Queue may already exist with conflicting properties;
            // fall back to consuming whatever's there.
            log.debug("AMQP queueDeclare for routing key {} fell back: {}", routingKey, e.message)
        }
        val incoming = ConcurrentLinkedQueue<ReceivedDelivery>()
        val consumer = object : DefaultConsumer(ch) {
            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                val headerBytes = (properties.headers ?: emptyMap()).mapValues { (_, v) ->
                    v?.toString()?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0)
                }
                incoming.add(ReceivedDelivery(body, headerBytes))
            }
        }
        ch.basicConsume(routingKey, true /* autoAck */, consumer)
        val sub = Subscription(routingKey, incoming)
        subscriptions[routingKey] = sub
        return sub
    }

    private fun drainOne(sub: Subscription, deadline: Long): ReceivedDelivery? {
        // Poll the consumer's queue in a short busy-wait. The rabbit
        // client's consumer callback fires on its own I/O thread so
        // by the time we ask, the queue may already have entries.
        while (System.currentTimeMillis() < deadline) {
            val d = sub.incoming.poll()
            if (d != null) return d
            try {
                TimeUnit.MILLISECONDS.sleep(25L.coerceAtMost(
                    (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
                ))
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }
        return null
    }
}
