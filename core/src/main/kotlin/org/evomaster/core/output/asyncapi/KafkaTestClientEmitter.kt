package org.evomaster.core.output.asyncapi

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.core.EMConfig
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Writes the Kafka client code that republishes one message, for a generated test, and that
 * observes what the service emits as a result.
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
     * What the helper that starts observing the topics the service emits to is named.
     */
    const val OBSERVE_HELPER_NAME = "observeEmitted"

    /**
     * What the helper that collects what the service emitted, once published, is named.
     */
    const val AWAIT_HELPER_NAME = "awaitEmitted"

    /**
     * What the variable holding a server's address is called in a generated test, before the
     * server's own name.
     */
    const val SERVER_VARIABLE_PREFIX = "asyncApiServer_"

    private const val STRING_SERDE = "org.apache.kafka.common.serialization"

    private const val UTF_8 = "java.nio.charset.StandardCharsets.UTF_8"

    /**
     * The classes of the payloads whose recorded text is what goes on the wire: a string, and
     * bytes, which the instrumentation decodes as UTF-8, as the test's consumer does. Anything
     * else is turned into bytes by whatever serializer the service configured, so its text, if
     * one was recorded, says nothing about what a consumer reads.
     */
    private val TEXT_PAYLOAD_TYPES = setOf(String::class.java.name, ByteArray::class.java.name)

    /**
     * What a failure to publish is caught as in a generated test, to close the observer before
     * it is thrown on.
     */
    private const val FAILURE_VARIABLE = "failure"

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

        val call = "$OBSERVE_HELPER_NAME(${(listOf(broker) + topics.map { quoted(it, format) }).joinToString(", ")})"

        when {
            format.isJava() -> lines.add("AutoCloseable $observer = $call;")
            format.isKotlin() -> lines.add("val $observer = $call")
            format.isPython() -> lines.add("$observer = self.$call")
            else -> throw IllegalStateException("No Kafka client is written for $format")
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
            format.isPython() -> "[$counts]"
            else -> throw IllegalStateException("No Kafka client is written for $format")
        }
        val args = listOf(
            observer,
            expected,
            timeoutMs.toString() + if (format.isPython()) "" else "L"
        ) + topics.keys.map { quoted(it, format) }

        val call = "$AWAIT_HELPER_NAME(${args.joinToString(", ")})"

        when {
            format.isJava() -> lines.add("java.util.Map<String, java.util.List<String>> $emitted = $call;")
            format.isKotlin() -> lines.add("val $emitted = $call")
            format.isPython() -> lines.add("$emitted = self.$call")
            else -> throw IllegalStateException("No Kafka client is written for $format")
        }
    }

    /**
     * Assertions on what [emitAwait] collected, from what the search saw the service emit.
     *
     * On every topic, that at least as many messages arrived as were seen: a service may also
     * emit on its own schedule. The payload is asserted on, as a reply is, only for a topic the
     * search saw get exactly one message, and only if the test collected just one there too:
     * order is only kept within a partition, and an extra message, on the service's own schedule
     * or late for an earlier one, could be the one at the front. A payload that is not text on
     * the wire, or was only partly recorded, is not asserted on, and the test says why.
     *
     * @param payloadPrefix what the variable holding a payload is called, before the topic's
     *                      position among the observed ones
     * @param bodyPrefix    the same, for the parsed payload
     */
    fun emitEmittedAssertions(
        lines: Lines,
        result: AsyncApiCallResult,
        emitted: String,
        payloadPrefix: String,
        bodyPrefix: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        observedTopicsOf(result).entries.forEachIndexed { i, (topic, messages) ->

            val onTopic = when {
                format.isJava() -> "$emitted.get(${quoted(topic, format)})"
                format.isKotlin() -> "$emitted.getValue(${quoted(topic, format)})"
                format.isPython() -> "$emitted[${quoted(topic, format)}]"
                else -> throw IllegalStateException("No Kafka client is written for $format")
            }

            when {
                format.isJava() -> lines.add("assertTrue($onTopic.size() >= ${messages.size});")
                format.isKotlin() -> lines.add("assertTrue($onTopic.size >= ${messages.size})")
                format.isPython() -> lines.add("assert len($onTopic) >= ${messages.size}")
            }

            if (messages.size != 1) {
                lines.addSingleCommentLine(
                    "${messages.size} messages were emitted to $topic, which may arrive in any order," +
                            " so their payloads are not asserted on"
                )
                return@forEachIndexed
            }

            val message = messages.single()
            val payload = message.payload

            val type = message.payloadType

            //no payload and no type: the service sent a null value, a tombstone
            if (payload == null && type == null) {
                lines.addSingleCommentLine(
                    "the message emitted to $topic had a null value, so there is no payload to assert on"
                )
                return@forEachIndexed
            }

            if (payload == null) {
                lines.addSingleCommentLine(
                    "the payload emitted to $topic was a $type turned into bytes inside the client," +
                            " so it is not asserted on"
                )
                return@forEachIndexed
            }

            if (type != null && type !in TEXT_PAYLOAD_TYPES) {
                lines.addSingleCommentLine(
                    "the payload emitted to $topic was a $type, which the service's serializer turns into" +
                            " bytes of its own, so it is not asserted on"
                )
                return@forEachIndexed
            }

            if (message.payloadTruncated == true) {
                lines.addSingleCommentLine(
                    "only the start of the payload emitted to $topic was recorded, so it is not asserted on"
                )
                return@forEachIndexed
            }

            val variable = payloadPrefix + i
            val assertions = Lines(format)
            AsyncApiReplyAssertions.emit(assertions, payload, variable, bodyPrefix + i, config, format)

            //no block that would only hold a comment, or nothing at all
            val comment = if (format.isPython()) "#" else "//"
            val asserts = assertions.toString().lines().any { it.isNotBlank() && !it.trimStart().startsWith(comment) }
            if (!asserts) {
                lines.add(assertions)
                return@forEachIndexed
            }

            lines.addSingleCommentLine("only when it arrived alone, as any other could be the one at the front")
            when {
                format.isJava() -> lines.add("if ($onTopic.size() == 1) {")
                format.isKotlin() -> lines.add("if ($onTopic.size == 1) {")
                format.isPython() -> lines.add("if len($onTopic) == 1:")
            }
            lines.indented {
                when {
                    format.isJava() -> lines.add("String $variable = $onTopic.get(0);")
                    format.isKotlin() -> lines.add("val $variable = $onTopic[0]")
                    format.isPython() -> lines.add("$variable = $onTopic[0]")
                }
                lines.add(assertions)
            }
            if (!format.isPython()) {
                lines.add("}")
            }
        }
    }

    /**
     * The call one action becomes, once [emitHelper] has written what it calls.
     *
     * @param variable what the reply payload is left in, named by the core, and what the other
     *                 names here are keyed off so two actions cannot collide
     * @param broker   where to publish, as an expression: the name of a variable the suite
     *                 filled in from the driver, or a literal taken from the document
     * @param observer what [emitObserve] left the observer in, if it was called for this result.
     *                 Where it started one, a failure to publish closes it before it is thrown
     *                 on, since [emitAwait], which closes it otherwise, is never reached
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

        if (observer == null || observedTopicsOf(result).isEmpty()) {
            when {
                format.isJava() -> lines.add("String $variable = $call;")
                format.isKotlin() -> lines.add("val $variable = $call")
                format.isPython() -> lines.add("$variable = self.$call")
                else -> throw IllegalStateException("No Kafka client is written for $format")
            }
            return
        }

        when {
            format.isJava() -> {
                lines.add("String $variable;")
                lines.add("try {")
                lines.indented { lines.add("$variable = $call;") }
                lines.add("} catch (Throwable $FAILURE_VARIABLE) {")
                lines.indented {
                    lines.add("$observer.close();")
                    lines.add("throw $FAILURE_VARIABLE;")
                }
                lines.add("}")
            }
            format.isKotlin() -> {
                lines.add("val $variable = try {")
                lines.indented { lines.add(call) }
                lines.add("} catch ($FAILURE_VARIABLE: Throwable) {")
                lines.indented {
                    lines.add("$observer.close()")
                    lines.add("throw $FAILURE_VARIABLE")
                }
                lines.add("}")
            }
            format.isPython() -> {
                lines.add("try:")
                lines.indented { lines.add("$variable = self.$call") }
                lines.add("except BaseException:")
                lines.indented {
                    lines.add("$observer.close()")
                    lines.add("raise")
                }
            }
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
        writeMember(lines, when {
            format.isJava() -> javaHelper()
            format.isPython() -> pythonHelper()
            format.isKotlin() -> kotlinHelper()
            else -> throw IllegalStateException("No Kafka client is written for $format")
        })
    }

    /**
     * Write the helpers that observe what the service emits, once for the suite. Only for a suite
     * where [observesIn] holds, next to the one [emitHelper] writes.
     *
     * The Python form is written too, to keep the three in step, though nothing reaches it yet:
     * only a white-box run records what the service emits, and those write Java or Kotlin.
     */
    fun emitObserveHelpers(lines: Lines, format: OutputFormat) {
        when {
            format.isJava() -> {
                writeMember(lines, javaObserveHelper())
                writeMember(lines, javaAwaitHelper())
            }
            format.isPython() -> {
                writeMember(lines, pythonObserveHelper())
                writeMember(lines, pythonAwaitHelper())
            }
            format.isKotlin() -> {
                writeMember(lines, kotlinObserveHelper())
                writeMember(lines, kotlinAwaitHelper())
            }
            else -> throw IllegalStateException("No Kafka client is written for $format")
        }
    }

    private fun writeMember(lines: Lines, body: List<String>) {
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
        "                #seek_to_end is lazy: asking for the position is what makes it take effect",
        "                #now, rather than on the first poll, by when the reply would be behind it",
        "                consumer.position(partitions[0])",
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
    private fun javaObserveHelper(): List<String> = listOf(
        "/**",
        " * Start observing what the service emits to the given topics, from their current end, so",
        " * that only what a message published afterwards makes it emit is collected. A topic that",
        " * does not exist yet is picked up from its start once it does, by $AWAIT_HELPER_NAME.",
        " * Close it, or hand it to $AWAIT_HELPER_NAME, which does.",
        " */",
        "private static AutoCloseable $OBSERVE_HELPER_NAME(String broker, String... topics) {",
        "    java.util.Properties props = new java.util.Properties();",
        "    props.put(\"bootstrap.servers\", broker);",
        "    props.put(\"enable.auto.commit\", \"false\");",
        "    org.apache.kafka.clients.consumer.KafkaConsumer<String,String> consumer =",
        "            new org.apache.kafka.clients.consumer.KafkaConsumer<>(props,",
        "                    new $STRING_SERDE.StringDeserializer(), new $STRING_SERDE.StringDeserializer());",
        "    try {",
        "        java.util.List<org.apache.kafka.common.TopicPartition> parts = new java.util.ArrayList<>();",
        "        for (String topic : topics) {",
        "            java.util.List<org.apache.kafka.common.PartitionInfo> info = consumer.partitionsFor(topic);",
        "            for (org.apache.kafka.common.PartitionInfo p : info == null",
        "                    ? java.util.Collections.<org.apache.kafka.common.PartitionInfo>emptyList() : info) {",
        "                parts.add(new org.apache.kafka.common.TopicPartition(p.topic(), p.partition()));",
        "            }",
        "        }",
        "        if (!parts.isEmpty()) {",
        "            consumer.assign(parts);",
        "            consumer.seekToEnd(parts);",
        "            //seekToEnd is lazy: asking for the positions is what makes it take effect now",
        "            for (org.apache.kafka.common.TopicPartition p : parts) {",
        "                consumer.position(p);",
        "            }",
        "        }",
        "        return consumer;",
        "    } catch (RuntimeException e) {",
        "        consumer.close();",
        "        throw e;",
        "    }",
        "}"
    )

    private fun javaAwaitHelper(): List<String> = listOf(
        "/**",
        " * Collect what the service emitted to the given topics since $OBSERVE_HELPER_NAME, until",
        " * each topic got as many messages as expected holds for it, at the same position, or",
        " * timeoutMs went by, and close the observer.",
        " *",
        " * @return topic -> payloads emitted to it, in arrival order, with every topic present",
        " */",
        "private static java.util.Map<String, java.util.List<String>> $AWAIT_HELPER_NAME(AutoCloseable observer,",
        "        int[] expected, long timeoutMs, String... topics) throws Exception {",
        "    @SuppressWarnings(\"unchecked\")",
        "    org.apache.kafka.clients.consumer.KafkaConsumer<String,String> consumer =",
        "            (org.apache.kafka.clients.consumer.KafkaConsumer<String,String>) observer;",
        "    java.util.Map<String, java.util.List<String>> emitted = new java.util.LinkedHashMap<>();",
        "    for (String topic : topics) {",
        "        emitted.put(topic, new java.util.ArrayList<>());",
        "    }",
        "    long deadline = System.currentTimeMillis() + timeoutMs;",
        "    try {",
        "        while (System.currentTimeMillis() < deadline) {",
        "            boolean arrived = true;",
        "            for (int i = 0; i < topics.length; i++) {",
        "                if (emitted.get(topics[i]).size() < expected[i]) {",
        "                    arrived = false;",
        "                }",
        "            }",
        "            if (arrived) {",
        "                break;",
        "            }",
        "            java.util.Set<org.apache.kafka.common.TopicPartition> assigned =",
        "                    new java.util.LinkedHashSet<>(consumer.assignment());",
        "            java.util.Set<String> known = new java.util.HashSet<>();",
        "            for (org.apache.kafka.common.TopicPartition p : assigned) {",
        "                known.add(p.topic());",
        "            }",
        "            //a topic created since observing began only holds what was emitted since: read it all",
        "            java.util.List<org.apache.kafka.common.TopicPartition> created = new java.util.ArrayList<>();",
        "            for (String topic : topics) {",
        "                java.util.List<org.apache.kafka.common.PartitionInfo> info =",
        "                        known.contains(topic) ? null : consumer.partitionsFor(topic);",
        "                for (org.apache.kafka.common.PartitionInfo p : info == null",
        "                        ? java.util.Collections.<org.apache.kafka.common.PartitionInfo>emptyList() : info) {",
        "                    created.add(new org.apache.kafka.common.TopicPartition(p.topic(), p.partition()));",
        "                }",
        "            }",
        "            if (!created.isEmpty()) {",
        "                assigned.addAll(created);",
        "                consumer.assign(assigned);",
        "                consumer.seekToBeginning(created);",
        "            }",
        "            if (assigned.isEmpty()) {",
        "                Thread.sleep($DEFAULT_POLL_MS);",
        "                continue;",
        "            }",
        "            for (org.apache.kafka.clients.consumer.ConsumerRecord<String,String> r :",
        "                    consumer.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {",
        "                java.util.List<String> payloads = emitted.get(r.topic());",
        "                if (payloads != null) {",
        "                    payloads.add(r.value());",
        "                }",
        "            }",
        "        }",
        "        return emitted;",
        "    } finally {",
        "        consumer.close();",
        "    }",
        "}"
    )

    private fun kotlinObserveHelper(): List<String> = listOf(
        "/**",
        " * Start observing what the service emits to the given topics, from their current end, so",
        " * that only what a message published afterwards makes it emit is collected. A topic that",
        " * does not exist yet is picked up from its start once it does, by $AWAIT_HELPER_NAME.",
        " * Close it, or hand it to $AWAIT_HELPER_NAME, which does.",
        " */",
        "private fun $OBSERVE_HELPER_NAME(broker: String, vararg topics: String): AutoCloseable {",
        "    val props = java.util.Properties()",
        "    props.put(\"bootstrap.servers\", broker)",
        "    props.put(\"enable.auto.commit\", \"false\")",
        "    val consumer = org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(props,",
        "            $STRING_SERDE.StringDeserializer(), $STRING_SERDE.StringDeserializer())",
        "    try {",
        "        val parts = topics.flatMap { t -> (consumer.partitionsFor(t) ?: listOf())",
        "                .map { org.apache.kafka.common.TopicPartition(it.topic(), it.partition()) } }",
        "        if (parts.isNotEmpty()) {",
        "            consumer.assign(parts)",
        "            consumer.seekToEnd(parts)",
        "            //seekToEnd is lazy: asking for the positions is what makes it take effect now",
        "            parts.forEach { consumer.position(it) }",
        "        }",
        "        return consumer",
        "    } catch (e: RuntimeException) {",
        "        consumer.close()",
        "        throw e",
        "    }",
        "}"
    )

    private fun kotlinAwaitHelper(): List<String> = listOf(
        "/**",
        " * Collect what the service emitted to the given topics since $OBSERVE_HELPER_NAME, until",
        " * each topic got as many messages as expected holds for it, at the same position, or",
        " * timeoutMs went by, and close the observer.",
        " *",
        " * @return topic -> payloads emitted to it, in arrival order, with every topic present",
        " */",
        "private fun $AWAIT_HELPER_NAME(observer: AutoCloseable, expected: IntArray, timeoutMs: Long,",
        //spelt in full: a generated Kotlin suite imports java.util.List, as every suite does
        "        vararg topics: String): Map<String, kotlin.collections.List<String?>> {",
        "    @Suppress(\"UNCHECKED_CAST\")",
        "    val consumer = observer as org.apache.kafka.clients.consumer.KafkaConsumer<String, String>",
        "    val emitted = topics.associateWith { mutableListOf<String?>() }",
        "    val deadline = System.currentTimeMillis() + timeoutMs",
        "    try {",
        "        while (System.currentTimeMillis() < deadline",
        "                && topics.indices.any { emitted.getValue(topics[it]).size < expected[it] }) {",
        "            val assigned = consumer.assignment().toMutableSet()",
        "            val known = assigned.map { it.topic() }.toSet()",
        "            //a topic created since observing began only holds what was emitted since: read it all",
        "            val created = topics.filter { it !in known }.flatMap { t -> (consumer.partitionsFor(t) ?: listOf())",
        "                    .map { org.apache.kafka.common.TopicPartition(it.topic(), it.partition()) } }",
        "            if (created.isNotEmpty()) {",
        "                assigned.addAll(created)",
        "                consumer.assign(assigned)",
        "                consumer.seekToBeginning(created)",
        "            }",
        "            if (assigned.isEmpty()) {",
        "                Thread.sleep($DEFAULT_POLL_MS)",
        "                continue",
        "            }",
        "            for (r in consumer.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {",
        "                emitted[r.topic()]?.add(r.value())",
        "            }",
        "        }",
        "        return emitted",
        "    } finally {",
        "        consumer.close()",
        "    }",
        "}"
    )

    private fun pythonObserveHelper(): List<String> = listOf(
        "@staticmethod",
        "def $OBSERVE_HELPER_NAME(broker, *topics):",
        "    \"\"\"",
        "    Start observing what the service emits to the given topics, from their current end, so",
        "    that only what a message published afterwards makes it emit is collected. A topic that",
        "    does not exist yet is picked up from its start once it does, by $AWAIT_HELPER_NAME.",
        "    Close it, or hand it to $AWAIT_HELPER_NAME, which does.",
        "    \"\"\"",
        "    consumer = kafka.KafkaConsumer(bootstrap_servers=broker, enable_auto_commit=False)",
        "    try:",
        "        partitions = [kafka.TopicPartition(t, p)",
        "                for t in topics for p in (consumer.partitions_for_topic(t) or [])]",
        "        if partitions:",
        "            consumer.assign(partitions)",
        "            consumer.seek_to_end(*partitions)",
        "            #seek_to_end is lazy: asking for the positions is what makes it take effect now",
        "            for p in partitions:",
        "                consumer.position(p)",
        "        return consumer",
        "    except Exception:",
        "        consumer.close()",
        "        raise"
    )

    private fun pythonAwaitHelper(): List<String> = listOf(
        "@staticmethod",
        "def $AWAIT_HELPER_NAME(observer, expected, timeout_ms, *topics):",
        "    \"\"\"",
        "    Collect what the service emitted to the given topics since $OBSERVE_HELPER_NAME, until",
        "    each topic got as many messages as expected holds for it, at the same position, or",
        "    timeout_ms went by, and close the observer.",
        "    Returns topic -> payloads emitted to it, in arrival order, with every topic present.",
        "    \"\"\"",
        "    emitted = {t: [] for t in topics}",
        "    deadline = time.time() + (timeout_ms / 1000.0)",
        "    try:",
        "        while time.time() < deadline and any(len(emitted[t]) < n for t, n in zip(topics, expected)):",
        "            assigned = set(observer.assignment())",
        "            known = set(p.topic for p in assigned)",
        "            #a topic created since observing began only holds what was emitted since: read it all",
        "            created = [kafka.TopicPartition(t, p) for t in topics if t not in known",
        "                    for p in (observer.partitions_for_topic(t) or [])]",
        "            if created:",
        "                assigned.update(created)",
        "                observer.assign(list(assigned))",
        "                observer.seek_to_beginning(*created)",
        "            if not assigned:",
        "                time.sleep($DEFAULT_POLL_MS / 1000.0)",
        "                continue",
        "            for tp, records in observer.poll(timeout_ms=$DEFAULT_POLL_MS).items():",
        "                for r in records:",
        "                    if r.topic in emitted:",
        "                        emitted[r.topic].append(r.value.decode('utf-8') if r.value is not None else None)",
        "        return emitted",
        "    finally:",
        "        observer.close()"
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
