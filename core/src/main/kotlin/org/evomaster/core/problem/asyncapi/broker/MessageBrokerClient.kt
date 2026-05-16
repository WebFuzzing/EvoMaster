package org.evomaster.core.problem.asyncapi.broker

/**
 * Protocol-agnostic message broker bridge used by the AsyncAPI fitness layer.
 *
 * The interface is deliberately narrow — publish, subscribe with a header
 * predicate, close — because that is everything an AsyncAPI 3.0
 * request/reply round trip needs.  Per-protocol concerns (consumer offsets,
 * QoS levels, ack semantics) stay inside the implementations.
 *
 * Only Kafka is implemented in the starter slice; the abstraction's value is
 * keeping the fitness function free of broker types so MQTT/AMQP can be added
 * later without refactoring.
 */
interface MessageBrokerClient : AutoCloseable {

    /** Connect to the broker. Idempotent: calling twice is a no-op. */
    fun connect()

    /**
     * Publish [payload] (UTF-8 JSON bytes for now) with the given key and
     * headers to [channel].  Returns when the broker has acknowledged delivery.
     */
    fun publish(channel: String, key: String?, headers: Map<String, ByteArray>, payload: ByteArray): PublishOutcome

    /**
     * Block up to [timeoutMs] waiting for the first message on [channel] whose
     * headers satisfy [predicate].  Returns [SubscribeOutcome.Timeout] if no
     * matching message arrives in time.
     */
    fun awaitFirstMatching(
        channel: String,
        predicate: (Map<String, ByteArray>) -> Boolean,
        timeoutMs: Long
    ): SubscribeOutcome

    /**
     * Subscribe to [channel] and collect every message that arrives during
     * the [windowMs] millisecond listen window. Returns the collected messages
     * in arrival order (possibly empty).
     *
     * Implementations should not filter by correlation header — the caller
     * decides what to do with the collected payloads. Used by AsyncAPI
     * output-observation oracles (M9-PR4) where the schema doesn't encode
     * causality between a publish and an SUT-emitted event, so the engine
     * brackets a fixed window after publishing and inspects whatever showed up.
     */
    fun collectAllWithin(channel: String, windowMs: Long): List<ReceivedMessage>

    /** Close all underlying resources.  Subsequent calls are no-ops. */
    override fun close()

    sealed class PublishOutcome {
        data class Sent(val protocolMetadata: Map<String, String>) : PublishOutcome()
        data class Failed(val reason: String) : PublishOutcome()
    }

    sealed class SubscribeOutcome {
        data class Received(val payload: ByteArray, val headers: Map<String, ByteArray>) : SubscribeOutcome()
        data object Timeout : SubscribeOutcome()
    }

    /** A single message captured by [collectAllWithin]. */
    data class ReceivedMessage(val payload: ByteArray, val headers: Map<String, ByteArray>)
}
