package org.evomaster.core.problem.asyncapi.broker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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

    /**
     * Wire-format envelope strategies the AMQP client knows about.
     *
     * - [NONE]: AsyncAPI channel is treated as a routing key on the default
     *   exchange, payload published and received verbatim. The starter slice
     *   for M11-PR9 and the convention every JSON-Schema-based bookworm-free
     *   SUT in the corpus is wired against.
     * - [MASSTRANSIT]: the AsyncAPI channel is treated as a *MassTransit
     *   fanout exchange name* (per-message-type), and every publish wraps the
     *   payload in `{messageId, messageType: ["urn:message:<channel>"],
     *   message: <inner>}` with content-type
     *   `application/vnd.masstransit+json`. On receive, the envelope is
     *   unwrapped before being handed back to the caller. Required to drive
     *   .NET MassTransit consumers (bookworm trio) — without this they
     *   silently skip every message because the bytes don't match the
     *   serializer's expected shape.
     */
    enum class EnvelopeStrategy {
        NONE, MASSTRANSIT;

        companion object {
            /**
             * Map an AsyncAPI document's `defaultContentType` to an envelope
             * strategy. Unknown / null content types fall back to [NONE].
             *
             * Only the document-level field is consulted today; per-message
             * content-type overrides are an M12 follow-up if a corpus member
             * ever mixes envelopes within one schema.
             */
            fun fromContentType(contentType: String?): EnvelopeStrategy {
                val ct = contentType?.trim()?.lowercase() ?: return NONE
                return when {
                    ct.startsWith("application/vnd.masstransit+json") -> MASSTRANSIT
                    else -> NONE
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AmqpBrokerClient::class.java)
        private val threadCounter = AtomicLong(0)
        private val mapper = ObjectMapper()
        private const val MASSTRANSIT_CONTENT_TYPE = "application/vnd.masstransit+json"

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

        /**
         * Build a daemon ThreadFactory. The default amqp-client
         * threads (the I/O thread, the consumer worker pool, the
         * heartbeat sender) are non-daemon, which means the JVM
         * waits for them on shutdown even after `Connection.close()`
         * has been called and the application's main thread has
         * returned. EvoMaster's harness sees that as a process
         * that completes the search successfully but then hangs
         * indefinitely between SUTs in a sweep.
         *
         * Forcing daemon threads lets the JVM exit cleanly as soon
         * as the main thread is done; the AMQP broker connection
         * dies with the process. Acceptable for a fuzzer-side
         * client where reconnects between actions are routed through
         * a fresh AmqpBrokerClient per fitness evaluation anyway.
         */
        private fun daemonThreadFactory(prefix: String): ThreadFactory = ThreadFactory { r ->
            Thread(r, "$prefix-${threadCounter.incrementAndGet()}").apply {
                isDaemon = true
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

    /**
     * Envelope strategy in effect for publishes and receives. Set once via
     * [configureContentType] during fitness setup; defaults to [EnvelopeStrategy.NONE]
     * so unconfigured clients (older tests, runs against non-MassTransit SUTs)
     * keep the M11-PR9 default-exchange behaviour.
     *
     * Public so unit tests can configure the strategy directly without parsing
     * an AsyncAPI document.
     */
    var envelopeStrategy: EnvelopeStrategy = EnvelopeStrategy.NONE

    /**
     * Adopt the envelope strategy implied by the AsyncAPI document's
     * `defaultContentType`. Idempotent — calling with the same content type
     * twice is a no-op; conflicting calls log a warning and keep the last
     * value (lets per-test overrides win over schema auto-detection).
     */
    override fun configureContentType(defaultContentType: String?) {
        val resolved = EnvelopeStrategy.fromContentType(defaultContentType)
        if (resolved != envelopeStrategy) {
            log.debug(
                "AMQP envelope strategy {} -> {} (defaultContentType={})",
                envelopeStrategy, resolved, defaultContentType
            )
            envelopeStrategy = resolved
        }
    }

    override fun connect() {
        if (connection != null) return
        val uri = resolveUri(bootstrapUri)
        connectionFactory.setUri(uri)
        // Force daemon I/O + consumer worker threads so the JVM can
        // exit cleanly when the engine finishes. See [daemonThreadFactory]
        // for the rationale.
        connectionFactory.threadFactory = daemonThreadFactory("evm-amqp-io")
        connectionFactory.setSharedExecutor(
            Executors.newCachedThreadPool(daemonThreadFactory("evm-amqp-worker"))
        )
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

            val (exchange, routingKey, finalPayload, contentType) = when (envelopeStrategy) {
                EnvelopeStrategy.NONE -> {
                    // Default exchange ("") + channel as routing key.
                    PublishWire("", channel, payload, "application/json")
                }
                EnvelopeStrategy.MASSTRANSIT -> {
                    // MassTransit publishes to a per-message-type fanout
                    // exchange named after the message type (= the AsyncAPI
                    // channel address). Routing key is unused for fanout.
                    // The exchange must exist before publishing; we declare
                    // it idempotently so we don't depend on the SUT having
                    // booted first.
                    ensureFanoutExchange(channel)
                    val envelope = wrapMassTransit(channel, payload)
                    PublishWire(channel, "", envelope, MASSTRANSIT_CONTENT_TYPE)
                }
            }

            val props = AMQP.BasicProperties.Builder()
                .contentType(contentType)
                .headers(headerTable)
                .build()
            this.channel!!.basicPublish(exchange, routingKey, props, finalPayload)
            MessageBrokerClient.PublishOutcome.Sent(
                mapOf(
                    "exchange" to exchange,
                    "routingKey" to routingKey,
                    "bytes" to finalPayload.size.toString(),
                    "envelope" to envelopeStrategy.name
                )
            )
        } catch (e: Exception) {
            log.warn("AMQP publish to {} failed: {}", channel, e.message)
            MessageBrokerClient.PublishOutcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private data class PublishWire(
        val exchange: String,
        val routingKey: String,
        val payload: ByteArray,
        val contentType: String
    )

    /**
     * Wrap [innerPayload] in the MassTransit envelope. The `messageType` URN
     * is derived from the AsyncAPI channel address: MassTransit's convention
     * (`urn:message:<Namespace>:<TypeName>`) lines up with the BookWorm
     * schemas' channel naming (`BookWorm.Contracts:BasketDeletedFailed...`),
     * so the same string serves both roles.
     *
     * The inner payload is parsed as JSON and re-emitted nested. If parsing
     * fails (caller passed non-JSON bytes), the envelope falls back to a
     * string field — defensive, the engine shouldn't be doing that.
     */
    internal fun wrapMassTransit(channel: String, innerPayload: ByteArray): ByteArray {
        val envelope: ObjectNode = mapper.createObjectNode().apply {
            put("messageId", UUID.randomUUID().toString())
            putArray("messageType").add("urn:message:$channel")
            val parsed: JsonNode? = try {
                mapper.readTree(innerPayload)
            } catch (e: Exception) {
                log.debug("MassTransit wrap: inner payload not parseable as JSON: {}", e.message)
                null
            }
            if (parsed != null) {
                set<JsonNode>("message", parsed)
            } else {
                put("message", String(innerPayload, StandardCharsets.UTF_8))
            }
        }
        return mapper.writeValueAsBytes(envelope)
    }

    /**
     * Unwrap a received MassTransit envelope, returning the inner `message`
     * payload bytes. If the bytes aren't a recognisable envelope (e.g. the
     * SUT published a raw payload despite the document declaring MassTransit
     * content type), the original bytes are returned unchanged. This lets
     * unit tests assert on observable behaviour rather than crashing.
     */
    internal fun unwrapMassTransit(envelope: ByteArray): ByteArray {
        return try {
            val node = mapper.readTree(envelope)
            val message = node.get("message")
            when {
                message == null -> envelope
                message.isTextual -> message.asText().toByteArray(StandardCharsets.UTF_8)
                else -> mapper.writeValueAsBytes(message)
            }
        } catch (e: Exception) {
            log.debug("MassTransit unwrap: bytes not a JSON envelope: {}", e.message)
            envelope
        }
    }

    private fun ensureFanoutExchange(name: String) {
        // Declare an exchange that matches MassTransit's defaults: durable,
        // not auto-delete (MassTransit's consumers re-declare exchanges with
        // the same args; mismatched declarations fail with 406 PRECONDITION_FAILED).
        // We use BuiltinExchangeType.FANOUT to mirror MassTransit's per-message-type
        // exchange topology.
        this.channel!!.exchangeDeclare(name, BuiltinExchangeType.FANOUT, true, false, null)
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
    private fun ensureSubscribed(channelAddress: String): Subscription {
        subscriptions[channelAddress]?.let { return it }
        val ch = channel!!

        val queueName: String = when (envelopeStrategy) {
            EnvelopeStrategy.NONE -> {
                // Default-exchange routing: declare a queue named after the
                // channel address (idempotent). The default exchange auto-binds
                // by queue name, so messages published with routingKey=<name>
                // land here. Matches M11-PR9's starter behaviour.
                try {
                    ch.queueDeclare(channelAddress, false, false, true, null)
                } catch (e: Exception) {
                    log.debug(
                        "AMQP queueDeclare for routing key {} fell back: {}",
                        channelAddress, e.message
                    )
                }
                channelAddress
            }
            EnvelopeStrategy.MASSTRANSIT -> {
                // MassTransit topology: the channel address is a per-message-type
                // fanout exchange. We need a server-named exclusive queue bound
                // to that exchange so we receive every message the SUT publishes
                // to it (and any we publish ourselves for self-observation).
                ensureFanoutExchange(channelAddress)
                val declared = ch.queueDeclare("", false, true, true, null)
                ch.queueBind(declared.queue, channelAddress, "")
                declared.queue
            }
        }

        val incoming = ConcurrentLinkedQueue<ReceivedDelivery>()
        val strategy = envelopeStrategy
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
                val effectiveBody = when (strategy) {
                    EnvelopeStrategy.NONE -> body
                    EnvelopeStrategy.MASSTRANSIT -> unwrapMassTransit(body)
                }
                incoming.add(ReceivedDelivery(effectiveBody, headerBytes))
            }
        }
        ch.basicConsume(queueName, true /* autoAck */, consumer)
        val sub = Subscription(queueName, incoming)
        subscriptions[channelAddress] = sub
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
