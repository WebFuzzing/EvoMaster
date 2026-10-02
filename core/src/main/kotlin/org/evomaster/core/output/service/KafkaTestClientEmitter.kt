package org.evomaster.core.output.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Writes the Kafka client code that republishes one message, for a generated test.
 *
 * This is the AsyncAPI counterpart of the RestAssured calls the REST writer emits: the test
 * talks to the broker itself, with an ordinary `kafka-clients` producer and consumer, and needs
 * no EvoMaster driver at run time. Everything it needs was resolved from the contract while the
 * search ran and kept on the result, so nothing is read from the document again here.
 *
 * Only Kafka is written this way. A transport that carries its correlation id inside the
 * service's own message layout cannot be emitted from the contract alone, because the contract
 * does not describe that layout; there, the driver renders the lines instead.
 *
 * On the JVM everything emitted here names its types in full rather than adding imports to the
 * suite, which is how the REST writer reaches the one method RestAssured does not expose. The
 * suite's imports are decided before it is known whether anything will publish over Kafka, and
 * adding them unconditionally would make every AsyncAPI suite need the dependency, including one
 * that only ever talks to a socket through a driver. Python has no such spelling, so its import
 * is written with the helper instead, by [TestSuiteWriter], under the same condition.
 */
object KafkaTestClientEmitter {

    private val log: Logger = LoggerFactory.getLogger(KafkaTestClientEmitter::class.java)

    /**
     * The protocols this can write. The document names them on the server.
     */
    private val KAFKA_PROTOCOLS = setOf("kafka", "kafka-secure")

    private const val DEFAULT_POLL_MS = 200L

    /**
     * What the helper the tests call is named.
     */
    const val HELPER_NAME = "publishAndAwaitReply"

    /**
     * What the variable holding a server's address is called in a generated test, before the
     * server's own name.
     */
    const val SERVER_VARIABLE_PREFIX = "asyncApiServer_"

    private const val STRING_SERDE = "org.apache.kafka.common.serialization"

    private const val UTF_8 = "java.nio.charset.StandardCharsets.UTF_8"

    private val mapper = ObjectMapper()

    /**
     * What stands for an absent value in the target language.
     */
    private fun nothing(format: OutputFormat) = if (format.isPython()) "None" else "null"

    /**
     * A name or an address out of the document, as a literal of the target language.
     */
    fun brokerLiteral(address: String, format: OutputFormat): String =
        quoted(address, format)

    /**
     * Whether this result carries enough, and the right transport, to be written as Kafka code.
     *
     * A correlation id that rides inside the service's own message layout is the case this
     * cannot write: the document says where in the payload it goes, but the payload recorded
     * here is the one the search composed, without it. A test written from that would publish a
     * message no reply could answer, and -- having no id to match on -- would take whatever
     * happened to be on the reply destination for its answer. Left to the driver instead.
     */
    fun canEmit(result: AsyncApiCallResult): Boolean =
        result.getProtocol()?.lowercase() in KAFKA_PROTOCOLS
                && !result.getBroker().isNullOrBlank()
                && !result.getAddress().isNullOrBlank()
                && (result.getReplyAddress().isNullOrBlank() || result.getCorrelationHeader() != null)

    /**
     * The results of a solution this can write Kafka code for: the ones whose driver left the
     * publishing to it. One predicate, so that what declares the helper, what imports it, and
     * what calls it cannot disagree.
     */
    fun resultsIn(solution: Solution<*>): List<AsyncApiCallResult> =
        solution.individuals
            .flatMap { it.evaluatedMainActions() }
            .mapNotNull { it.result as? AsyncApiCallResult }
            .filter { r -> r.getTestScript().none { it.isNotBlank() } && canEmit(r) }

    /**
     * The call one action becomes, once [emitHelper] has written what it calls.
     *
     * @param variable what the reply payload is left in, named by the core, and what the other
     *                 names here are keyed off so two actions cannot collide
     * @param broker   where to publish, as an expression: the name of a variable the suite
     *                 filled in from the driver, or a literal taken from the document
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
            quoted(result.getAddress()!!, format),
            result.getReplyAddress()?.let { quoted(it, format) } ?: nothing(format),
            result.getPayload()?.let { quoted(it, format) } ?: nothing(format),
            result.getCorrelationHeader()?.let { quoted(it, format) } ?: nothing(format),
            (result.getReplyTimeoutMs() ?: 0L).toString() + if (format.isPython()) "" else "L"
        )

        //the message's own headers, flattened so that one call still fits on one line
        headersOf(result).forEach { (key, value) ->
            args.add(quoted(key, format))
            args.add(quoted(value, format))
        }

        val call = "$HELPER_NAME(${args.joinToString(", ")})"

        when {
            format.isJava() -> lines.add("String $variable = $call;")
            format.isKotlin() -> lines.add("val $variable = $call")
            format.isPython() -> lines.add("$variable = self.$call")
            else -> throw IllegalStateException("No Kafka client is written for $format")
        }
    }

    /**
     * Write the helper the tests call, once for the suite.
     *
     * It is a method rather than lines repeated in every test because the scaffolding a broker
     * needs is long and identical each time, and a test that reads in one line is worth more
     * than one that spells out a producer.
     */
    fun emitHelper(lines: Lines, format: OutputFormat) {
        val body = when {
            format.isJava() -> javaHelper()
            format.isPython() -> pythonHelper()
            format.isKotlin() -> kotlinHelper()
            else -> throw IllegalStateException("No Kafka client is written for $format")
        }
        lines.addEmpty()
        body.forEach { lines.add(it) }
        lines.addEmpty()
    }

    private fun javaHelper(): List<String> = listOf(
        "/**",
        " * Publish one message and, when a reply is declared, wait for the one that answers it.",
        " *",
        " * The consumer seeks to the end of the reply topic before anything is published, so a",
        " * reply left over from an earlier run is never taken for an answer to this one, and a",
        " * fresh correlation id is minted per call for the same reason.",
        " */",
        "private static String $HELPER_NAME(String broker, String topic, String replyTopic, String payload,",
        "        String correlationHeader, long timeoutMs, String... headerPairs) throws Exception {",
        "    java.util.Properties props = new java.util.Properties();",
        "    props.put(\"bootstrap.servers\", broker);",
        "    props.put(\"auto.offset.reset\", \"latest\");",
        "    props.put(\"enable.auto.commit\", \"false\");",
        "    String correlationId = java.util.UUID.randomUUID().toString();",
        "    org.apache.kafka.clients.consumer.KafkaConsumer<String,String> consumer = null;",
        "    org.apache.kafka.clients.producer.KafkaProducer<String,String> producer = null;",
        "    try {",
        "        if (replyTopic != null) {",
        "            consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props,",
        "                    new $STRING_SERDE.StringDeserializer(), new $STRING_SERDE.StringDeserializer());",
        "            java.util.List<org.apache.kafka.common.TopicPartition> parts = new java.util.ArrayList<>();",
        "            java.util.List<org.apache.kafka.common.PartitionInfo> info = consumer.partitionsFor(replyTopic);",
        "            for (org.apache.kafka.common.PartitionInfo p : info == null",
        "                    ? java.util.Collections.<org.apache.kafka.common.PartitionInfo>emptyList() : info) {",
        "                parts.add(new org.apache.kafka.common.TopicPartition(p.topic(), p.partition()));",
        "            }",
        "            if (parts.isEmpty()) {",
        "                //the reply destination does not exist yet, so nothing can be waiting on it",
        "                consumer.close();",
        "                consumer = null;",
        "            } else {",
        "                consumer.assign(parts);",
        "                consumer.seekToEnd(parts);",
        "                //seekToEnd is lazy: asking for the position is what makes it take effect now",
        "                consumer.position(parts.get(0));",
        "            }",
        "        }",
        "        producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props,",
        "                new $STRING_SERDE.StringSerializer(), new $STRING_SERDE.StringSerializer());",
        "        //a message with no payload of its own publishes an empty body",
        "        org.apache.kafka.clients.producer.ProducerRecord<String,String> record =",
        "                new org.apache.kafka.clients.producer.ProducerRecord<>(topic, payload == null ? \"\" : payload);",
        "        for (int i = 0; i + 1 < headerPairs.length; i += 2) {",
        "            record.headers().add(headerPairs[i], headerPairs[i + 1].getBytes($UTF_8));",
        "        }",
        "        if (correlationHeader != null) {",
        "            record.headers().add(correlationHeader, correlationId.getBytes($UTF_8));",
        "        }",
        "        producer.send(record).get();",
        "        if (consumer == null) {",
        "            return null;",
        "        }",
        "        String reply = null;",
        "        long deadline = System.currentTimeMillis() + timeoutMs;",
        "        while (reply == null && System.currentTimeMillis() < deadline) {",
        "            for (org.apache.kafka.clients.consumer.ConsumerRecord<String,String> r :",
        "                    consumer.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {",
        "                if (correlationHeader == null) {",
        "                    reply = r.value();",
        "                    continue;",
        "                }",
        "                org.apache.kafka.common.header.Header h = r.headers().lastHeader(correlationHeader);",
        "                if (h != null && correlationId.equals(new String(h.value(), $UTF_8))) {",
        "                    reply = r.value();",
        "                }",
        "            }",
        "        }",
        "        return reply;",
        "    } finally {",
        "        //a suite runs many of these, so a client left open would outlive every one of them",
        "        if (producer != null) {",
        "            producer.close();",
        "        }",
        "        if (consumer != null) {",
        "            consumer.close();",
        "        }",
        "    }",
        "}"
    )

    /**
     * The Python form, against kafka-python. A Python suite has no driver to ask, since the
     * controller is Java, so it always publishes to the address the document declares.
     */
    private fun pythonHelper(): List<String> = listOf(
        "@staticmethod",
        "def $HELPER_NAME(broker, topic, reply_topic, payload, correlation_header, timeout_ms, *header_pairs):",
        "    \"\"\"",
        "    Publish one message and, when a reply is declared, wait for the one that answers it.",
        "",
        "    The consumer seeks to the end of the reply topic before anything is published, so a",
        "    reply left over from an earlier run is never taken for an answer to this one, and a",
        "    fresh correlation id is minted per call for the same reason.",
        "    \"\"\"",
        "    correlation_id = str(uuid.uuid4())",
        "    consumer = None",
        "    producer = None",
        "    try:",
        "        if reply_topic is not None:",
        "            consumer = kafka.KafkaConsumer(bootstrap_servers=broker,",
        "                    auto_offset_reset='latest', enable_auto_commit=False)",
        "            partitions = [kafka.TopicPartition(reply_topic, p)",
        "                    for p in (consumer.partitions_for_topic(reply_topic) or [])]",
        "            if not partitions:",
        "                #the reply destination does not exist yet, so nothing can be waiting on it",
        "                consumer.close()",
        "                consumer = None",
        "            else:",
        "                consumer.assign(partitions)",
        "                consumer.seek_to_end()",
        "        producer = kafka.KafkaProducer(bootstrap_servers=broker)",
        "        headers = []",
        "        i = 0",
        "        while i + 1 < len(header_pairs):",
        "            headers.append((header_pairs[i], header_pairs[i + 1].encode('utf-8')))",
        "            i += 2",
        "        if correlation_header is not None:",
        "            headers.append((correlation_header, correlation_id.encode('utf-8')))",
        "        #a message with no payload of its own publishes an empty body",
        "        body = (payload if payload is not None else '').encode('utf-8')",
        "        producer.send(topic, value=body, headers=headers)",
        "        producer.flush()",
        "        if consumer is None:",
        "            return None",
        "        reply = None",
        "        deadline = time.time() + (timeout_ms / 1000.0)",
        "        while reply is None and time.time() < deadline:",
        "            for _, records in consumer.poll(timeout_ms=$DEFAULT_POLL_MS).items():",
        "                for r in records:",
        "                    if correlation_header is None:",
        "                        reply = r.value.decode('utf-8')",
        "                        continue",
        "                    for key, value in (r.headers or []):",
        "                        if key == correlation_header and value.decode('utf-8') == correlation_id:",
        "                            reply = r.value.decode('utf-8')",
        "        return reply",
        "    finally:",
        "        #a suite runs many of these, so a client left open would outlive every one of them",
        "        if producer is not None:",
        "            producer.close()",
        "        if consumer is not None:",
        "            consumer.close()"
    )

    private fun kotlinHelper(): List<String> = listOf(
        "/**",
        " * Publish one message and, when a reply is declared, wait for the one that answers it.",
        " *",
        " * The consumer seeks to the end of the reply topic before anything is published, so a",
        " * reply left over from an earlier run is never taken for an answer to this one, and a",
        " * fresh correlation id is minted per call for the same reason.",
        " */",
        "private fun $HELPER_NAME(broker: String, topic: String, replyTopic: String?, payload: String?,",
        "        correlationHeader: String?, timeoutMs: Long, vararg headerPairs: String): String? {",
        "    val props = java.util.Properties()",
        "    props.put(\"bootstrap.servers\", broker)",
        "    props.put(\"auto.offset.reset\", \"latest\")",
        "    props.put(\"enable.auto.commit\", \"false\")",
        "    val correlationId = java.util.UUID.randomUUID().toString()",
        "    var consumer: org.apache.kafka.clients.consumer.KafkaConsumer<String, String>? = null",
        "    var producer: org.apache.kafka.clients.producer.KafkaProducer<String, String>? = null",
        "    try {",
        "        if (replyTopic != null) {",
        "            val c = org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(props,",
        "                    $STRING_SERDE.StringDeserializer(), $STRING_SERDE.StringDeserializer())",
        "            consumer = c",
        "            val parts = (c.partitionsFor(replyTopic) ?: listOf())",
        "                    .map { org.apache.kafka.common.TopicPartition(it.topic(), it.partition()) }",
        "            if (parts.isEmpty()) {",
        "                //the reply destination does not exist yet, so nothing can be waiting on it",
        "                c.close()",
        "                consumer = null",
        "            } else {",
        "                c.assign(parts)",
        "                c.seekToEnd(parts)",
        "                //seekToEnd is lazy: asking for the position is what makes it take effect now",
        "                c.position(parts[0])",
        "            }",
        "        }",
        "        val p = org.apache.kafka.clients.producer.KafkaProducer<String, String>(props,",
        "                $STRING_SERDE.StringSerializer(), $STRING_SERDE.StringSerializer())",
        "        producer = p",
        "        //a message with no payload of its own publishes an empty body",
        "        val record = org.apache.kafka.clients.producer.ProducerRecord<String, String>(topic, payload ?: \"\")",
        "        var i = 0",
        "        while (i + 1 < headerPairs.size) {",
        "            record.headers().add(headerPairs[i], headerPairs[i + 1].toByteArray($UTF_8))",
        "            i += 2",
        "        }",
        "        if (correlationHeader != null) {",
        "            record.headers().add(correlationHeader, correlationId.toByteArray($UTF_8))",
        "        }",
        "        p.send(record).get()",
        "        val c = consumer ?: return null",
        "        var reply: String? = null",
        "        val deadline = System.currentTimeMillis() + timeoutMs",
        "        while (reply == null && System.currentTimeMillis() < deadline) {",
        "            for (r in c.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {",
        "                if (correlationHeader == null) {",
        "                    reply = r.value()",
        "                    continue",
        "                }",
        "                val h = r.headers().lastHeader(correlationHeader)",
        "                if (h != null && correlationId == String(h.value(), $UTF_8)) {",
        "                    reply = r.value()",
        "                }",
        "            }",
        "        }",
        "        return reply",
        "    } finally {",
        "        //a suite runs many of these, so a client left open would outlive every one of them",
        "        producer?.close()",
        "        consumer?.close()",
        "    }",
        "}"
    )
    /**
     * The message's own headers, as the search sent them.
     */
    private fun headersOf(result: AsyncApiCallResult): Map<String, String> {

        val json = result.getHeadersAsJson() ?: return mapOf()

        return try {
            val node = mapper.readTree(json)
            node.fieldNames().asSequence().associateWith { node.get(it).asText() }
        } catch (e: Exception) {
            /*
                The search recorded these, so this should not happen; said out loud rather than
                quietly publishing a message without the headers the service may route on.
             */
            LoggingUtil.uniqueWarn(log, "Could not read the headers recorded for a message: ${e.message}")
            mapOf()
        }
    }

    /**
     * A string as a literal of the target language, with what would end it escaped.
     */
    private fun quoted(value: String, format: OutputFormat): String {

        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .let { if (format.isKotlin()) it.replace("$", "\\$") else it }

        return "\"$escaped\""
    }
}
