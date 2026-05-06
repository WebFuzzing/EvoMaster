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
}
