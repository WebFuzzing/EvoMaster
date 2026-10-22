package org.evomaster.core.output.asyncapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.search.Solution
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
    private val KAFKA_PROTOCOLS = setOf("kafka", "kafka-secure")

    /**
     * Class in test-utils-java holding the helper. Named as a string, as core does not depend on
     * kafka-clients, which the class needs to be loaded.
     */
    const val UTILS_CLASS = "org.evomaster.test.utils.KafkaEMUtils"

    const val HELPER_NAME = "publishAndAwaitReply"

    /**
     * Prefix of the variable holding a server's address in a generated test, followed by the
     * server's name.
     */
    const val SERVER_VARIABLE_PREFIX = "asyncApiServer_"

    private val mapper = ObjectMapper()

    /**
     * Whether this result carries enough, and the right transport, to be written as Kafka code.
     *
     * Not the case when the correlation id goes inside the payload: the recorded payload is the
     * one the search composed, without the id, so the generated test could not match the reply.
     * The driver renders those instead.
     */
    fun canEmit(result: AsyncApiCallResult): Boolean =
        result.getProtocol()?.lowercase() in KAFKA_PROTOCOLS
                && !result.getServerHost().isNullOrBlank()
                && !result.getAddress().isNullOrBlank()
                && (result.getReplyAddress().isNullOrBlank() || result.getCorrelationHeader() != null)

    /**
     * The results of a solution written as Kafka calls: those the driver rendered no lines for.
     */
    fun resultsIn(solution: Solution<*>): List<AsyncApiCallResult> =
        solution.individuals
            .flatMap { it.evaluatedMainActions() }
            .mapNotNull { it.result as? AsyncApiCallResult }
            .filter { r -> r.getTestScript().none { it.isNotBlank() } && canEmit(r) }

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
        format: OutputFormat
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

        val call = "$HELPER_NAME(${args.joinToString(", ")})"

        when {
            format.isJava() -> lines.add("String $variable = $call;")
            format.isKotlin() -> lines.add("val $variable = $call")
            else -> throw IllegalArgumentException("No Kafka call is written for $format")
        }
    }

    /**
     * The message's own headers, as the search sent them.
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
