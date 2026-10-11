package org.evomaster.core.problem.asyncapi.data

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.problem.enterprise.EnterpriseActionResult
import org.evomaster.core.search.action.Action

/**
 * What happened when one message was published: the outcome, and the reply when there was one.
 */
class AsyncApiCallResult : EnterpriseActionResult {

    companion object {
        const val OUTCOME = "OUTCOME"
        const val REPLY_PAYLOAD = "REPLY_PAYLOAD"
        const val REPLY_MESSAGE = "REPLY_MESSAGE"
        const val CORRELATION_MATCHED = "CORRELATION_MATCHED"
        const val WAITED_MS = "WAITED_MS"
        const val SETTLED_MS = "SETTLED_MS"
        const val SETTLE_ENDED_BY = "SETTLE_ENDED_BY"
        const val TEST_SCRIPT = "TEST_SCRIPT"
        const val EMITTED_MESSAGES = "EMITTED_MESSAGES"
        const val DECLARED_EMISSION_ADDRESSES = "DECLARED_EMISSION_ADDRESSES"

        /*
            What was published, kept so that a generated test can publish the same thing without
            resolving the document a second time. Only recorded when tests are being written.
         */
        const val SERVER_HOST = "SERVER_HOST"
        const val SERVER_NAME = "SERVER_NAME"
        const val PROTOCOL = "PROTOCOL"
        const val ADDRESS = "ADDRESS"
        const val REPLY_ADDRESS = "REPLY_ADDRESS"
        const val PAYLOAD = "PAYLOAD"
        const val HEADERS = "HEADERS"
        const val CORRELATION_HEADER = "CORRELATION_HEADER"
        const val REPLY_TIMEOUT_MS = "REPLY_TIMEOUT_MS"
        const val REPLY_VARIABLE = "REPLY_VARIABLE"

        /**
         * What the script's lines are joined with when stored, and split on when read back.
         * A result holds strings, and a line of source never contains this.
         */
        private const val SCRIPT_SEPARATOR = "\n"

        /**
         * How many characters of the payload of an emitted message a result keeps. Results are
         * kept for every action in the archive, and the instrumentation's own cut, at a few
         * thousand characters, is more than a generated test needs. A payload cut here is
         * flagged as truncated, and is not asserted on.
         */
        const val EMITTED_PAYLOAD_MAX_LENGTH = 1024

        /**
         * Writes and reads the emitted messages, which a result can only hold as a string.
         */
        private val mapper = ObjectMapper()

        private val emittedMessagesType = object : TypeReference<List<EmittedMessageDto>>() {}

        private val addressesType = object : TypeReference<List<String>>() {}
    }

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    private constructor(other: AsyncApiCallResult) : super(other)

    override fun copy(): AsyncApiCallResult {
        return AsyncApiCallResult(this)
    }

    override fun matchedType(action: Action): Boolean {
        return action is AsyncApiAction
    }

    fun setOutcome(outcome: AsyncApiOutcome) {
        addResultValue(OUTCOME, outcome.name)
    }

    fun getOutcome(): AsyncApiOutcome? = getResultValue(OUTCOME)?.let { AsyncApiOutcome.valueOf(it) }

    fun setReplyPayload(payload: String) {
        addResultValue(REPLY_PAYLOAD, payload)
    }

    fun getReplyPayload(): String? = getResultValue(REPLY_PAYLOAD)

    /**
     * Which of the messages the contract declares for the reply this one was recognised as.
     */
    fun setReplyMessage(messageId: String) {
        addResultValue(REPLY_MESSAGE, messageId)
    }

    fun getReplyMessage(): String? = getResultValue(REPLY_MESSAGE)

    fun setCorrelationMatched(matched: Boolean) {
        addResultValue(CORRELATION_MATCHED, matched.toString())
    }

    fun getCorrelationMatched(): Boolean? = getResultValue(CORRELATION_MATCHED)?.toBoolean()

    fun setWaitedMs(ms: Long) {
        addResultValue(WAITED_MS, ms.toString())
    }

    fun getWaitedMs(): Long? = getResultValue(WAITED_MS)?.toLong()

    /**
     * How long the action was kept open after the driver returned, so that what the service did
     * with the message was still credited to it. Absent when no such wait was asked for.
     */
    fun setSettledMs(ms: Long) {
        addResultValue(SETTLED_MS, ms.toString())
    }

    fun getSettledMs(): Long? = getResultValue(SETTLED_MS)?.toLong()

    /**
     * Why the controller stopped keeping the action open. Absent when no wait was asked for.
     */
    fun setSettleEndedBy(end: AsyncApiReplyDto.SettleEndReason) {
        addResultValue(SETTLE_ENDED_BY, end.name)
    }

    fun getSettleEndedBy(): AsyncApiReplyDto.SettleEndReason? =
        getResultValue(SETTLE_ENDED_BY)?.let { AsyncApiReplyDto.SettleEndReason.valueOf(it) }

    /**
     * The messages the service emitted while this one was being handled, in the order it emitted
     * them, as the instrumentation of its broker client recorded them. Only in white-box mode.
     *
     * Only what the search and the generated tests read is kept of each: the address, the
     * payload cut at [EMITTED_PAYLOAD_MAX_LENGTH] characters, whether it was cut, and the class
     * of the payload.
     */
    fun setEmittedMessages(messages: List<EmittedMessageDto>) {
        addResultValue(EMITTED_MESSAGES, mapper.writeValueAsString(messages.map { slim(it) }))
    }

    /**
     * A copy of [message] with only what a result keeps of it.
     */
    private fun slim(message: EmittedMessageDto): EmittedMessageDto {

        val payload = message.payload
        val cut = payload != null && payload.length > EMITTED_PAYLOAD_MAX_LENGTH

        return EmittedMessageDto().apply {
            address = message.address
            payloadType = message.payloadType
            if (cut) {
                //not between the two halves of a surrogate pair, which would leave half a character
                val end = if (Character.isHighSurrogate(payload!![EMITTED_PAYLOAD_MAX_LENGTH - 1])) {
                    EMITTED_PAYLOAD_MAX_LENGTH - 1
                } else {
                    EMITTED_PAYLOAD_MAX_LENGTH
                }
                this.payload = payload.substring(0, end)
                payloadTruncated = true
            } else {
                this.payload = payload
                payloadTruncated = message.payloadTruncated
            }
        }
    }

    /**
     * @return the messages the service emitted while this one was being handled, or empty when
     * none was recorded
     */
    fun getEmittedMessages(): List<EmittedMessageDto> =
        getResultValue(EMITTED_MESSAGES)?.let { mapper.readValue(it, emittedMessagesType) } ?: listOf()

    /**
     * The addresses among those of [getEmittedMessages] that the contract declares for the
     * service to send to, in the order first emitted to. Not the reply address of this message.
     * Only in white-box mode.
     */
    fun setDeclaredEmissionAddresses(addresses: List<String>) {
        addResultValue(DECLARED_EMISSION_ADDRESSES, mapper.writeValueAsString(addresses))
    }

    /**
     * @return the addresses the contract declares for the service to send to that it emitted to
     * while this message was being handled, or empty when there were none
     */
    fun getDeclaredEmissionAddresses(): List<String> =
        getResultValue(DECLARED_EMISSION_ADDRESSES)?.let { mapper.readValue(it, addressesType) } ?: listOf()

    /**
     * The lines the driver rendered for a generated test, or empty when it rendered none.
     */
    fun setTestScript(lines: List<String>) {
        addResultValue(TEST_SCRIPT, lines.joinToString(SCRIPT_SEPARATOR))
    }

    fun getTestScript(): List<String> =
        getResultValue(TEST_SCRIPT)?.split(SCRIPT_SEPARATOR) ?: listOf()

    fun setReplyVariableName(name: String) {
        addResultValue(REPLY_VARIABLE, name)
    }

    fun getReplyVariableName(): String? = getResultValue(REPLY_VARIABLE)

    /**
     * Everything a generated test needs to publish this message again, as the search resolved it
     * from the document. Headers are a JSON object, so that one string holds them all.
     */
    fun setPublishDetails(
        serverName: String?,
        serverHost: String?,
        protocol: String?,
        address: String?,
        replyAddress: String?,
        payload: String?,
        headersAsJson: String,
        correlationHeader: String?,
        replyTimeoutMs: Long?
    ) {
        serverName?.let { addResultValue(SERVER_NAME, it) }
        serverHost?.let { addResultValue(SERVER_HOST, it) }
        protocol?.let { addResultValue(PROTOCOL, it) }
        address?.let { addResultValue(ADDRESS, it) }
        replyAddress?.let { addResultValue(REPLY_ADDRESS, it) }
        payload?.let { addResultValue(PAYLOAD, it) }
        addResultValue(HEADERS, headersAsJson)
        correlationHeader?.let { addResultValue(CORRELATION_HEADER, it) }
        replyTimeoutMs?.let { addResultValue(REPLY_TIMEOUT_MS, it.toString()) }
    }

    /**
     * The server's `host` as declared in the document, eg "localhost:9092".
     */
    fun getServerHost(): String? = getResultValue(SERVER_HOST)

    /**
     * The key of the server in the document, which is what the driver is asked about when a
     * generated test needs to know where that server is now.
     */
    fun getServerName(): String? = getResultValue(SERVER_NAME)

    fun getProtocol(): String? = getResultValue(PROTOCOL)

    fun getAddress(): String? = getResultValue(ADDRESS)

    fun getReplyAddress(): String? = getResultValue(REPLY_ADDRESS)

    fun getPayload(): String? = getResultValue(PAYLOAD)

    fun getHeadersAsJson(): String? = getResultValue(HEADERS)

    fun getCorrelationHeader(): String? = getResultValue(CORRELATION_HEADER)

    fun getReplyTimeoutMs(): Long? = getResultValue(REPLY_TIMEOUT_MS)?.toLong()
}
