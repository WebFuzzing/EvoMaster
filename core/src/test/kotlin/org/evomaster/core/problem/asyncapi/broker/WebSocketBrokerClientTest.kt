package org.evomaster.core.problem.asyncapi.broker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * M11-PR8: confirms the URL-joining contract that lets AsyncAPI channel
 * addresses combine with the configured `--bbBrokerUrl` base origin to
 * produce a single WebSocket endpoint URI.
 *
 * Absolute `ws://` / `wss://` channel addresses bypass the join and are
 * returned verbatim — useful when the schema declares a fully-qualified
 * server URL inline.
 */
class WebSocketBrokerClientTest {

    @Test
    fun relativeChannelJoinsOntoOrigin() {
        val uri = WebSocketBrokerClient.resolveChannelUri(
            "ws://localhost:8080", "/events"
        )
        assertEquals("ws://localhost:8080/events", uri.toString())
    }

    @Test
    fun originTrailingSlashIsNormalised() {
        val uri = WebSocketBrokerClient.resolveChannelUri(
            "ws://localhost:8080/", "/events"
        )
        assertEquals("ws://localhost:8080/events", uri.toString())
    }

    @Test
    fun channelWithoutLeadingSlashGetsOne() {
        val uri = WebSocketBrokerClient.resolveChannelUri(
            "ws://localhost:8080", "events"
        )
        assertEquals("ws://localhost:8080/events", uri.toString())
    }

    @Test
    fun absoluteWsChannelBypassesJoin() {
        val uri = WebSocketBrokerClient.resolveChannelUri(
            "ws://localhost:8080", "ws://other-host:9090/abs"
        )
        assertEquals("ws://other-host:9090/abs", uri.toString())
    }

    @Test
    fun absoluteWssChannelBypassesJoin() {
        val uri = WebSocketBrokerClient.resolveChannelUri(
            "ws://localhost:8080", "wss://secure.example.com/notifications"
        )
        assertEquals("wss://secure.example.com/notifications", uri.toString())
    }
}
