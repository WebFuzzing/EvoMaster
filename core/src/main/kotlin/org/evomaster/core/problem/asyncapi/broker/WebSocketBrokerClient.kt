package org.evomaster.core.problem.asyncapi.broker

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * WebSocket (RFC 6455) backing for [MessageBrokerClient].
 *
 * Uses the JDK 11+ `java.net.http.WebSocket` API, so no extra dependencies
 * are pulled into `core`. Each channel address is resolved against the
 * configured `--bbBrokerUrl` base origin (e.g. `ws://localhost:8080`);
 * absolute `ws://` / `wss://` addresses bypass the join.
 *
 * The MessageBrokerClient surface is broker-shaped (publish + subscribe
 * with headers). WebSocket frames don't have native headers, so this
 * client encodes headers into a JSON envelope of the form
 * `{ "headers": { … }, "payload": <base64-encoded-bytes> }`. Servers that
 * just want the bare payload can ignore the envelope; servers that want
 * to honour AsyncAPI-declared correlation can read it from
 * `headers.correlationId`. On the receive side the symmetric decoder
 * tries the envelope shape first and falls back to treating the entire
 * frame as the raw payload when the envelope isn't present.
 *
 * One connection per channel. The connection stays open between
 * publish / await calls so a server that streams replies on the same
 * socket (typical for AsyncAPI request/reply over ws) still works.
 */
class WebSocketBrokerClient(
    private val baseOrigin: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val handshakeTimeoutMs: Long = 5_000L
) : MessageBrokerClient {

    companion object {
        private val log = LoggerFactory.getLogger(WebSocketBrokerClient::class.java)
        private val mapper = ObjectMapper()

        /**
         * Resolve [channel] against [baseOrigin]. Absolute `ws://` / `wss://`
         * channels are returned untouched. Relative channels are joined to
         * the base after normalising trailing/leading slashes.
         *
         * Visible for tests so the join logic can be unit-tested without
         * spinning up a server.
         */
        fun resolveChannelUri(baseOrigin: String, channel: String): URI {
            if (channel.startsWith("ws://") || channel.startsWith("wss://")) {
                return URI.create(channel)
            }
            val origin = baseOrigin.trimEnd('/')
            val suffix = if (channel.startsWith("/")) channel else "/$channel"
            return URI.create("$origin$suffix")
        }
    }

    /**
     * Live connections, one per channel URI. Buffered incoming frames live
     * alongside the WebSocket handle; reads drain the queue first before
     * blocking on the next frame.
     */
    private data class Channel(
        val socket: WebSocket,
        val incoming: ConcurrentLinkedQueue<ByteArray>,
        val lock: Object
    )

    private val channels = HashMap<URI, Channel>()
    private var closed = false

    override fun connect() {
        // No-op: connections are opened lazily per channel because the
        // base origin alone does not identify a single endpoint URI.
    }

    override fun publish(
        channel: String,
        key: String?,
        headers: Map<String, ByteArray>,
        payload: ByteArray
    ): MessageBrokerClient.PublishOutcome {
        if (closed) return MessageBrokerClient.PublishOutcome.Failed("client closed")
        return try {
            val uri = resolveChannelUri(baseOrigin, channel)
            val conn = openOrReuse(uri)
            val frame = encodeFrame(headers, payload)
            conn.socket.sendText(frame, true).get(handshakeTimeoutMs, TimeUnit.MILLISECONDS)
            MessageBrokerClient.PublishOutcome.Sent(
                mapOf("uri" to uri.toString(), "frameBytes" to frame.length.toString())
            )
        } catch (e: Exception) {
            log.warn("WebSocket publish to {} failed: {}", channel, e.message)
            MessageBrokerClient.PublishOutcome.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    override fun awaitFirstMatching(
        channel: String,
        predicate: (Map<String, ByteArray>) -> Boolean,
        timeoutMs: Long
    ): MessageBrokerClient.SubscribeOutcome {
        if (closed) return MessageBrokerClient.SubscribeOutcome.Timeout
        val uri = resolveChannelUri(baseOrigin, channel)
        val conn = openOrReuse(uri)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val frame = drainOne(conn, deadline)
                ?: return MessageBrokerClient.SubscribeOutcome.Timeout
            val (headers, payload) = decodeFrame(frame)
            if (predicate(headers)) {
                return MessageBrokerClient.SubscribeOutcome.Received(payload, headers)
            }
        }
        return MessageBrokerClient.SubscribeOutcome.Timeout
    }

    override fun collectAllWithin(
        channel: String,
        windowMs: Long
    ): List<MessageBrokerClient.ReceivedMessage> {
        if (closed) return emptyList()
        val uri = resolveChannelUri(baseOrigin, channel)
        val conn = openOrReuse(uri)
        val collected = mutableListOf<MessageBrokerClient.ReceivedMessage>()
        val deadline = System.currentTimeMillis() + windowMs
        while (System.currentTimeMillis() < deadline) {
            val frame = drainOne(conn, deadline) ?: break
            val (headers, payload) = decodeFrame(frame)
            collected.add(MessageBrokerClient.ReceivedMessage(payload, headers))
        }
        return collected
    }

    override fun close() {
        closed = true
        channels.values.forEach { conn ->
            try {
                conn.socket.sendClose(WebSocket.NORMAL_CLOSURE, "evomaster shutdown")
                    .get(1, TimeUnit.SECONDS)
            } catch (e: Exception) {
                log.debug("WebSocket close to {} failed: {}", conn.socket, e.message)
            }
        }
        channels.clear()
    }

    @Synchronized
    private fun openOrReuse(uri: URI): Channel {
        channels[uri]?.let { return it }
        val incoming = ConcurrentLinkedQueue<ByteArray>()
        val lock = Object()
        val listener = QueueingListener(incoming, lock)
        val socket = try {
            httpClient.newWebSocketBuilder()
                .buildAsync(uri, listener)
                .get(handshakeTimeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw IllegalStateException(
                "WebSocket handshake to $uri failed within ${handshakeTimeoutMs}ms: ${e.message}", e
            )
        }
        val ch = Channel(socket, incoming, lock)
        channels[uri] = ch
        return ch
    }

    /**
     * Poll the channel's incoming queue, blocking up to [deadline] for a
     * frame. Returns null when the deadline elapses with no frame.
     */
    private fun drainOne(conn: Channel, deadline: Long): ByteArray? {
        // The listener fills `incoming` from the WebSocket's reader thread.
        // We synchronise on conn.lock so the queue poll + wait pair is
        // atomic relative to the listener's notify.
        synchronized(conn.lock) {
            while (System.currentTimeMillis() < deadline) {
                val frame = conn.incoming.poll()
                if (frame != null) return frame
                val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
                conn.lock.wait(remaining)
            }
        }
        return null
    }

    private fun encodeFrame(headers: Map<String, ByteArray>, payload: ByteArray): String {
        if (headers.isEmpty()) {
            // Bare publish: send the payload as a raw UTF-8 text frame.
            return String(payload, StandardCharsets.UTF_8)
        }
        val node = mapper.createObjectNode()
        val headerObj = mapper.createObjectNode()
        headers.forEach { (k, v) ->
            headerObj.put(k, String(v, StandardCharsets.UTF_8))
        }
        node.set<com.fasterxml.jackson.databind.JsonNode>("headers", headerObj)
        node.put("payload", String(payload, StandardCharsets.UTF_8))
        return mapper.writeValueAsString(node)
    }

    private fun decodeFrame(frame: ByteArray): Pair<Map<String, ByteArray>, ByteArray> {
        // Try the envelope shape first. If the frame isn't JSON, isn't an
        // object, or lacks a `headers` field, treat the whole frame as
        // the raw payload.
        return try {
            val node = mapper.readTree(frame)
            if (node != null && node.isObject && node.has("headers") && node.has("payload")) {
                val hMap = LinkedHashMap<String, ByteArray>()
                node.get("headers").fields().forEachRemaining { (k, v) ->
                    if (v.isTextual) hMap[k] = v.asText().toByteArray(StandardCharsets.UTF_8)
                }
                val payloadText = node.get("payload").asText()
                hMap to payloadText.toByteArray(StandardCharsets.UTF_8)
            } else {
                emptyMap<String, ByteArray>() to frame
            }
        } catch (_: Exception) {
            emptyMap<String, ByteArray>() to frame
        }
    }

    /**
     * Listener that funnels every TEXT / BINARY frame into a thread-safe
     * queue, notifying any thread blocked in [drainOne]. Partial frames
     * are concatenated until `last = true`.
     */
    private class QueueingListener(
        private val incoming: ConcurrentLinkedQueue<ByteArray>,
        private val lock: Object
    ) : WebSocket.Listener {
        private val partialText = StringBuilder()
        private val partialBinary = java.io.ByteArrayOutputStream()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean
        ): CompletableFuture<*>? {
            partialText.append(data)
            if (last) {
                val frame = partialText.toString().toByteArray(StandardCharsets.UTF_8)
                partialText.setLength(0)
                enqueue(frame)
            }
            webSocket.request(1)
            return null
        }

        override fun onBinary(
            webSocket: WebSocket,
            data: java.nio.ByteBuffer,
            last: Boolean
        ): CompletableFuture<*>? {
            val bytes = ByteArray(data.remaining())
            data.get(bytes)
            partialBinary.write(bytes)
            if (last) {
                val frame = partialBinary.toByteArray()
                partialBinary.reset()
                enqueue(frame)
            }
            webSocket.request(1)
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletableFuture<*>? {
            synchronized(lock) { lock.notifyAll() }
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            log.debug("WebSocket listener error: {}", error.message)
            synchronized(lock) { lock.notifyAll() }
        }

        private fun enqueue(frame: ByteArray) {
            incoming.add(frame)
            synchronized(lock) { lock.notifyAll() }
        }
    }
}
