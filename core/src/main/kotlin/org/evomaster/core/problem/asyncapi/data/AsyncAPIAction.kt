package org.evomaster.core.problem.asyncapi.data

import org.evomaster.core.problem.api.ApiWsAction
import org.evomaster.core.problem.asyncapi.auth.AsyncAPINoAuth
import org.evomaster.core.problem.asyncapi.param.AsyncAPIParam
import org.evomaster.core.problem.asyncapi.schema.ReplyBinding
import org.evomaster.core.problem.enterprise.auth.AuthenticationInfo
import org.evomaster.core.search.gene.Gene

/**
 * One AsyncAPI 3.0 operation, instantiated for the search.
 *
 * Request/reply pairs are represented as **two** actions in the test case
 * (`PUBLISH` followed by `SUBSCRIBE_REPLY`) so the structure mutator can keep
 * them paired without inventing a new ActionGroup type.  The shared identity
 * is in [pairId].
 */
class AsyncAPIAction(
    /** Operation key from the schema (e.g. `sendNcsRequest`). */
    val operationName: String,
    /** Broker channel address used by this action (already resolved to topic). */
    val channelAddress: String,
    /** Channel key — useful for objective naming and for resolving variants. */
    val channelName: String,
    val kind: Kind,
    /** When set, this action ties to its publish/subscribe partner via the same id. */
    val pairId: String,
    /** Component message id this action carries. */
    val messageId: String,
    /**
     * For SUBSCRIBE_REPLY actions only: the *other* message ids the operation
     * declares as possible reply variants (`reply.messages: [...]`).  The
     * fitness layer tries every variant's schema when matching an incoming
     * reply, so a SUT that responds with any of N declared replies is fully
     * targeted.  Empty for PUBLISH actions and for replies that declare a
     * single message type.
     */
    val additionalReplyMessageIds: List<String> = emptyList(),
    /** Parameters owned by this action (payload, optional key, optional correlation id). */
    parameters: MutableList<AsyncAPIParam>,
    /** Reply binding for PUBLISH actions; null for SUBSCRIBE_REPLY. */
    val replyBinding: ReplyBinding? = null,
    /** Header where the correlation id is carried (mirrors the AsyncAPI `correlationId.location`). */
    val correlationHeaderName: String? = null,
    /**
     * Pre-computed schema-derived assertions the test-case writer should emit
     * on the reply payload. Populated by the builder for SUBSCRIBE_REPLY
     * actions; empty for PUBLISH / SUBSCRIBE_OUTPUT. M9-PR5.
     */
    val replyFieldAssertions: List<ReplyFieldAssertion> = emptyList(),
    auth: AuthenticationInfo = AsyncAPINoAuth()
) : ApiWsAction(auth, false, parameters) {

    enum class Kind {
        /** EvoMaster publishes a message into the SUT's input channel. */
        PUBLISH,
        /** EvoMaster subscribes on a reply channel after a paired PUBLISH. */
        SUBSCRIBE_REPLY,
        /**
         * EvoMaster subscribes on a SUT-produced channel (an `action: RECEIVE`
         * operation under the codebase's convention) and collects whatever the
         * SUT publishes inside a fixed-duration listen window. Used by the M9-PR4
         * output-observation oracle to attribute schema-derivable targets to the
         * SUT's emitted events without requiring a `reply:` binding.
         */
        SUBSCRIBE_OUTPUT
    }

    override fun getName(): String = "${kind.name}:$operationName:$channelAddress"

    override fun seeTopGenes(): List<out Gene> = parameters.flatMap { it.seeGenes() }

    override fun copyContent(): AsyncAPIAction {
        val copies = parameters.asSequence()
            .map { it.copy() as AsyncAPIParam }
            .toMutableList()
        return AsyncAPIAction(
            operationName = operationName,
            channelAddress = channelAddress,
            channelName = channelName,
            kind = kind,
            pairId = pairId,
            messageId = messageId,
            additionalReplyMessageIds = additionalReplyMessageIds,
            parameters = copies,
            replyBinding = replyBinding,
            correlationHeaderName = correlationHeaderName,
            replyFieldAssertions = replyFieldAssertions,
            auth = auth
        )
    }

    /** Convenience accessor: the AsyncAPIParam holding the message payload. */
    fun payloadParam(): AsyncAPIParam? =
        parameters.firstOrNull { (it as AsyncAPIParam).name == PAYLOAD_PARAM } as AsyncAPIParam?

    /** Convenience accessor: the AsyncAPIParam holding the correlation id. */
    fun correlationParam(): AsyncAPIParam? =
        parameters.firstOrNull { (it as AsyncAPIParam).name == CORRELATION_PARAM } as AsyncAPIParam?

    /** Convenience accessor: the AsyncAPIParam holding the broker key, if any. */
    fun keyParam(): AsyncAPIParam? =
        parameters.firstOrNull { (it as AsyncAPIParam).name == KEY_PARAM } as AsyncAPIParam?

    /** Convenience accessor: the AsyncAPIParam holding user-defined Kafka headers (separate from correlation). */
    fun headersParam(): AsyncAPIParam? =
        parameters.firstOrNull { (it as AsyncAPIParam).name == HEADERS_PARAM } as AsyncAPIParam?

    /** All channel-parameter AsyncAPIParams, keyed by the AsyncAPI parameter name (sans prefix). */
    fun channelParams(): Map<String, AsyncAPIParam> {
        return parameters.asSequence()
            .map { it as AsyncAPIParam }
            .filter { it.name.startsWith(CHANNEL_PARAM_PREFIX) }
            .associateBy { it.name.removePrefix(CHANNEL_PARAM_PREFIX) }
    }

    companion object {
        const val PAYLOAD_PARAM = "payload"
        const val CORRELATION_PARAM = "correlationId"
        const val KEY_PARAM = "key"
        const val HEADERS_PARAM = "headers"
        /**
         * Channel-parameter AsyncAPIParams are stored under
         * `param:<paramName>` so they don't collide with payload/headers/key
         * and can be enumerated at publish time to render the templated
         * channel address.
         */
        const val CHANNEL_PARAM_PREFIX = "param:"
    }
}
