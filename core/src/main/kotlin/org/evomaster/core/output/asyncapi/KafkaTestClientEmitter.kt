package org.evomaster.core.output.asyncapi

import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.webfuzzing.asyncapi.models.AsyncApiChannel
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.search.Solution
import org.evomaster.test.utils.KafkaEMUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Writes the call that republishes one message over Kafka in a generated test.
 *
 * This is the AsyncAPI counterpart of the RestAssured calls the REST writer emits. The call goes
 * to `KafkaEMUtils` in `test-utils-java`, so the test needs no EvoMaster driver at run time.
 * Everything it needs was resolved from the contract during the search and kept on the result.
 *
 * Only Kafka is written this way. When the correlation id is inside the service's own message
 * layout, the contract does not describe where, so the driver renders the lines instead.
 */
object KafkaTestClientEmitter {

    private val log: Logger = LoggerFactory.getLogger(KafkaTestClientEmitter::class.java)

    /**
     * The protocols this can write, as named on the server in the document.
     */
    private val KAFKA_PROTOCOLS = setOf(AsyncApiChannel.KAFKA, "kafka-secure")

    /** Class in test-utils-java holding the helper used by generated JVM tests. */
    val UTILS_CLASS = KafkaEMUtils::class.java.name

    const val HELPER_NAME = "publishAndAwaitReply"
    const val OBSERVE_HELPER_NAME = "observeEmitted"
    const val AWAIT_HELPER_NAME = "awaitEmitted"

    /**
     * Prefix of the variable holding a server's address in a generated test, followed by the
     * server's name.
     */
    const val SERVER_VARIABLE_PREFIX = "asyncApiServer_"

    private val mapper = ObjectMapper()

    /**
     * Whether a server's protocol is one a Kafka call is written for.
     */
    fun isKafka(protocol: String?) = protocol?.lowercase() in KAFKA_PROTOCOLS

    /**
     * Whether this result is written as a Kafka call: the driver rendered no lines for it, and it
     * carries enough, over the right transport, to write one.
     *
     * Not the case when the correlation id goes inside the payload: the recorded payload is the
     * one the search composed, without the id, so the generated test could not match the reply.
     * The driver renders those instead.
     */
    fun isEmitted(result: AsyncApiCallResult): Boolean =
        result.getTestScript().isEmpty()
                && result.getReplyVariableName() != null
                && isKafka(result.getProtocol())
                && !result.getServerHost().isNullOrBlank()
                && !result.getAddress().isNullOrBlank()
                && (result.getReplyAddress().isNullOrBlank() || result.getCorrelationHeader() != null)

    /**
     * The results of a solution written as Kafka calls.
     */
    fun resultsIn(solution: Solution<*>): List<AsyncApiCallResult> =
        solution.individuals
            .flatMap { it.evaluatedMainActions() }
            .mapNotNull { it.result as? AsyncApiCallResult }
            .filter { isEmitted(it) }

    /**
     * What the service emitted to the addresses the contract declares for it to send to while
     * this message was handled: key is the topic, in the order first emitted to, and value is
     * what was emitted there, in order. Always empty outside white-box mode, where nothing
     * records what the service emits.
     *
     * The address the message itself is published to is left out, even when the contract also
     * declares the service to send there: the observer would collect the test's own message, and
     * take it for one the service emitted.
     */
    fun observedTopicsOf(result: AsyncApiCallResult): Map<String, List<EmittedMessageDto>> {

        val published = result.getAddress()
        val topics = result.getDeclaredEmissionAddresses().filter { it != published }

        if (topics.isEmpty()) {
            return mapOf()
        }

        val emitted = result.getEmittedMessages()

        return topics.associateWith { topic -> emitted.filter { it.address == topic } }
            .filterValues { it.isNotEmpty() }
    }

    /**
     * Whether some result this writes Kafka code for also has topics to observe, which is when
     * the helpers that observe them are needed. Derived from [resultsIn], so that the helpers are
     * only declared where something calls them.
     */
    fun observesIn(solution: Solution<*>): Boolean =
        resultsIn(solution).any { observedTopicsOf(it).isNotEmpty() }

    /**
     * Before publishing: start observing the topics the service emitted to, from their end, so
     * that only what this message makes it emit is collected afterwards. Nothing is written when
     * there is nothing to observe.
     *
     * @param observer what the observer is left in
     * @param broker   where the topics are, as an expression, as for [emit]: the service is
     *                 taken to emit to the broker it was published to
     */
    fun emitObserve(
        lines: Lines,
        result: AsyncApiCallResult,
        observer: String,
        broker: String,
        format: OutputFormat
    ) {
        val topics = observedTopicsOf(result).keys

        if (topics.isEmpty()) {
            return
        }

        val call = "$OBSERVE_HELPER_NAME(${(listOf(broker) + topics.map { TestWriterUtils.quotedEscaped(it, format) }).joinToString(", ")})"

        when {
            format.isJava() -> lines.add("AutoCloseable $observer = $call;")
            format.isKotlin() -> lines.add("val $observer = $call")
            else -> throw IllegalArgumentException("No Kafka client is written for $format")
        }
    }

    /**
     * After publishing: collect what the service emitted to the observed topics, waiting until
     * each of them got as many messages as the search saw the service emit there, or [timeoutMs]
     * went by. Counted per topic, so that more than expected on one does not end the wait before
     * another got its own. Nothing is written when there is nothing to observe.
     *
     * @param emitted what the collected messages are left in: topic -> payloads, in arrival order
     */
    fun emitAwait(
        lines: Lines,
        result: AsyncApiCallResult,
        observer: String,
        emitted: String,
        timeoutMs: Long,
        format: OutputFormat
    ) {
        val topics = observedTopicsOf(result)

        if (topics.isEmpty()) {
            return
        }

        //one count per topic, in the order the topics are passed in
        val counts = topics.values.joinToString(", ") { it.size.toString() }
        val expected = when {
            format.isJava() -> "new int[]{$counts}"
            format.isKotlin() -> "intArrayOf($counts)"
            else -> throw IllegalArgumentException("No Kafka client is written for $format")
        }
        val args = listOf(
            observer,
            expected,
            "${timeoutMs}L"
        ) + topics.keys.map { TestWriterUtils.quotedEscaped(it, format) }

        val call = "$AWAIT_HELPER_NAME(${args.joinToString(", ")})"

        when {
            format.isJava() -> lines.add("java.util.Map<String, java.util.List<String>> $emitted = $call;")
            format.isKotlin() -> lines.add("val $emitted = $call")
            else -> throw IllegalArgumentException("No Kafka client is written for $format")
        }
    }


    /**
     * @param variable where the reply payload is stored
     * @param broker   where to publish, as an expression: a variable the suite filled in from
     *                 the driver, or a literal taken from the document
     */
    fun emit(
        lines: Lines,
        result: AsyncApiCallResult,
        variable: String,
        broker: String,
        format: OutputFormat,
        observer: String? = null
    ) {

        val args = mutableListOf(
            broker,
            TestWriterUtils.quotedEscaped(result.getAddress()!!, format),
            result.getReplyAddress()?.let { TestWriterUtils.quotedEscaped(it, format) } ?: "null",
            result.getPayload()?.let { TestWriterUtils.quotedEscaped(it, format) } ?: "null",
            result.getCorrelationHeader()?.let { TestWriterUtils.quotedEscaped(it, format) } ?: "null",
            "${result.getReplyTimeoutMs() ?: 0L}L"
        )

        //the message's own headers, as name/value pairs
        headersOf(result).forEach { (key, value) ->
            args.add(TestWriterUtils.quotedEscaped(key, format))
            args.add(TestWriterUtils.quotedEscaped(value, format))
        }

        val guarded = observer != null && observedTopicsOf(result).isNotEmpty()
        val function = if (guarded) "publishAndAwaitReplyObserved" else HELPER_NAME
        val prefix = if (guarded) "$observer, " else ""
        val call = "$function($prefix${args.joinToString(", ")})"

        when {
            format.isJava() -> lines.add("String $variable = $call;")
            format.isKotlin() -> lines.add("val $variable = $call")
            else -> throw IllegalArgumentException("No Kafka call is written for $format")
        }
    }

    /**
     * The headers recorded during the search. Keys are header names; values are the
     * corresponding header values as text.
     */
    private fun headersOf(result: AsyncApiCallResult): Map<String, String> {

        val json = result.getHeadersAsJson() ?: return mapOf()

        return try {
            val node = mapper.readTree(json)
            node.fieldNames().asSequence().associateWith { node.get(it).asText() }
        } catch (e: Exception) {
            //should not happen, as the search recorded these
            LoggingUtil.uniqueWarn(log, "Could not read the headers recorded for a message: ${e.message}")
            mapOf()
        }
    }
}
