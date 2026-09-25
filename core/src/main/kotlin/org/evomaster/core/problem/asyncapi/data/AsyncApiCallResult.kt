package org.evomaster.core.problem.asyncapi.data

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
        const val TEST_SCRIPT = "TEST_SCRIPT"

        /*
            What was published, kept so that a generated test can publish the same thing without
            resolving the document a second time. Only recorded when tests are being written.
         */
        const val BROKER = "BROKER"
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
    fun setPublished(
        broker: String?,
        protocol: String?,
        address: String?,
        replyAddress: String?,
        payload: String?,
        headersAsJson: String,
        correlationHeader: String?,
        replyTimeoutMs: Long?
    ) {
        broker?.let { addResultValue(BROKER, it) }
        protocol?.let { addResultValue(PROTOCOL, it) }
        address?.let { addResultValue(ADDRESS, it) }
        replyAddress?.let { addResultValue(REPLY_ADDRESS, it) }
        payload?.let { addResultValue(PAYLOAD, it) }
        addResultValue(HEADERS, headersAsJson)
        correlationHeader?.let { addResultValue(CORRELATION_HEADER, it) }
        replyTimeoutMs?.let { addResultValue(REPLY_TIMEOUT_MS, it.toString()) }
    }

    fun getBroker(): String? = getResultValue(BROKER)

    fun getProtocol(): String? = getResultValue(PROTOCOL)

    fun getAddress(): String? = getResultValue(ADDRESS)

    fun getReplyAddress(): String? = getResultValue(REPLY_ADDRESS)

    fun getPayload(): String? = getResultValue(PAYLOAD)

    fun getHeadersAsJson(): String? = getResultValue(HEADERS)

    fun getCorrelationHeader(): String? = getResultValue(CORRELATION_HEADER)

    fun getReplyTimeoutMs(): Long? = getResultValue(REPLY_TIMEOUT_MS)?.toLong()
}
