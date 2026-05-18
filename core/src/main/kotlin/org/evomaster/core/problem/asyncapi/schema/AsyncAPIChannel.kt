package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode

/**
 * AsyncAPI 3.0 channel — broker topic/queue plus the message variants it carries.
 */
data class AsyncAPIChannel(
    /** Map key in `channels:` (e.g. `requestsNcs`). */
    val name: String,
    /**
     * Broker-side address, e.g. `requests.ncs`.  May contain parameter
     * placeholders such as `tenants/{tenantId}/orders`; resolution to a
     * concrete topic happens at publish time once the per-parameter genes
     * have been sampled.  Required by AsyncAPI 3.0 with `null` denoting an
     * unbound channel.
     */
    val address: String?,
    /** Message ids (component-level) declared on this channel, in declaration order. */
    val messageIds: List<String>,
    /**
     * AsyncAPI 3.0 channel parameters as written in the schema:
     * `parameters.<paramName>` → its schema/description node.  The address
     * placeholders `{paramName}` reference these.  Empty when the channel
     * has no parameters (the common case).
     */
    val parameters: Map<String, JsonNode> = emptyMap(),
    /**
     * Channel-local message key → component-level message id.  AsyncAPI 3.0
     * lets operations reference messages by their channel-local key
     * (e.g. `#/channels/spot/messages/error`) where the local key (`error`)
     * may differ from the underlying component message id (`errorMessage`).
     * Inline channel messages also live here — their value is the synthetic
     * component id under which the inline definition was promoted into
     * [AsyncAPISchema.messages].
     */
    val messageKeyMap: Map<String, String> = emptyMap()
)
