package org.evomaster.core.problem.asyncapi.schema

/**
 * AsyncAPI 3.0 channel — broker topic/queue plus the message variants it carries.
 */
data class AsyncAPIChannel(
    /** Map key in `channels:` (e.g. `requestsNcs`). */
    val name: String,
    /** Broker-side address, e.g. `requests.ncs`. Required by AsyncAPI 3.0 with `null` denoting an unbound channel. */
    val address: String?,
    /** Message ids (component-level) declared on this channel, in declaration order. */
    val messageIds: List<String>
)
