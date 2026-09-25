package org.evomaster.core.output.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult

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
 * The emitted code is deliberately self-contained per action rather than calling a shared
 * helper: a test that can be read top to bottom is worth more than a short one, and it keeps a
 * suite that never publishes to Kafka from carrying the dependency.
 */
object KafkaTestClientEmitter {

    /**
     * The protocols this can write. The document names them on the server.
     */
    private val KAFKA_PROTOCOLS = setOf("kafka", "kafka-secure")

    private const val DEFAULT_POLL_MS = 200L

    private val mapper = ObjectMapper()

    /**
     * Whether this result carries enough, and the right transport, to be written as Kafka code.
     */
    fun canEmit(result: AsyncApiCallResult): Boolean =
        result.getProtocol()?.lowercase() in KAFKA_PROTOCOLS
                && !result.getBroker().isNullOrBlank()
                && !result.getAddress().isNullOrBlank()

    /**
     * Publish the message again and, when one is expected, wait for the reply that answers it.
     *
     * @param variable what the reply payload is left in, named by the core
     * @param index of the action, so that two in one test do not collide
     */
    fun emit(lines: Lines, result: AsyncApiCallResult, variable: String, index: Int, format: OutputFormat) {

        val suffix = "_$index"
        val props = "asyncApiProps$suffix"
        val producer = "asyncApiProducer$suffix"
        val consumer = "asyncApiConsumer$suffix"
        val record = "asyncApiRecord$suffix"
        val parts = "asyncApiPartitions$suffix"
        val cid = "asyncApiCid$suffix"
        val deadline = "asyncApiDeadline$suffix"

        val broker = quoted(result.getBroker()!!, format)
        val topic = quoted(result.getAddress()!!, format)
        val replyTopic = result.getReplyAddress()
        val timeout = result.getReplyTimeoutMs() ?: 0L

        declareProperties(lines, props, broker, format)

        //a fresh id per run: a reply to an earlier run's message is not an answer to this one
        assign(lines, cid, "java.util.UUID.randomUUID().toString()", "String", format)

        val serializer = "org.apache.kafka.common.serialization.String"
        newInstance(
            lines, producer,
            "org.apache.kafka.clients.producer.KafkaProducer<String,String>",
            "org.apache.kafka.clients.producer.KafkaProducer<>($props, new ${serializer}Serializer(), new ${serializer}Serializer())",
            "org.apache.kafka.clients.producer.KafkaProducer($props, ${serializer}Serializer(), ${serializer}Serializer())",
            format
        )

        if (replyTopic != null) {
            newInstance(
                lines, consumer,
                "org.apache.kafka.clients.consumer.KafkaConsumer<String,String>",
                "org.apache.kafka.clients.consumer.KafkaConsumer<>($props, new ${serializer}Deserializer(), new ${serializer}Deserializer())",
                "org.apache.kafka.clients.consumer.KafkaConsumer($props, ${serializer}Deserializer(), ${serializer}Deserializer())",
                format
            )
            seekToEnd(lines, consumer, parts, quoted(replyTopic, format), format)
        }

        buildRecord(lines, record, topic, result, format)
        stampCorrelation(lines, record, cid, result, format)

        statement(lines, "$producer.send($record).get()", format)

        if (replyTopic == null) {
            close(lines, producer, format)
            //nothing is expected back, so the variable the core named stays unset
            declareReply(lines, variable, format)
            return
        }

        declareReply(lines, variable, format)
        awaitReply(lines, consumer, variable, cid, deadline, timeout, result, format)
        close(lines, producer, format)
        close(lines, consumer, format)
    }

    private fun declareProperties(lines: Lines, name: String, broker: String, format: OutputFormat) {
        newInstance(lines, name, "java.util.Properties", "java.util.Properties()", "java.util.Properties()", format)
        statement(lines, "$name.put(\"bootstrap.servers\", $broker)", format)
        //read only what is published from here on, never what was already on the topic
        statement(lines, "$name.put(\"auto.offset.reset\", \"latest\")", format)
        statement(lines, "$name.put(\"enable.auto.commit\", \"false\")", format)
    }

    private fun seekToEnd(lines: Lines, consumer: String, parts: String, topic: String, format: OutputFormat) {

        val expr = "$consumer.partitionsFor($topic).stream()" +
                ".map(p -> new org.apache.kafka.common.TopicPartition(p.topic(), p.partition()))" +
                ".collect(java.util.stream.Collectors.toList())"
        val kotlinExpr = "$consumer.partitionsFor($topic)" +
                ".map { org.apache.kafka.common.TopicPartition(it.topic(), it.partition()) }"

        when {
            format.isJava() -> lines.add("java.util.List<org.apache.kafka.common.TopicPartition> $parts = $expr;")
            format.isKotlin() -> lines.add("val $parts = $kotlinExpr")
        }

        statement(lines, "$consumer.assign($parts)", format)
        statement(lines, "$consumer.seekToEnd($parts)", format)
        //seekToEnd is lazy, so make it take effect before anything is published
        statement(lines, "$consumer.poll(java.time.Duration.ZERO)", format)
    }

    private fun buildRecord(lines: Lines, name: String, topic: String, result: AsyncApiCallResult, format: OutputFormat) {

        val payload = result.getPayload()?.let { quoted(it, format) } ?: nullLiteral(format)

        newInstance(
            lines, name,
            "org.apache.kafka.clients.producer.ProducerRecord<String,String>",
            "org.apache.kafka.clients.producer.ProducerRecord<>($topic, $payload)",
            "org.apache.kafka.clients.producer.ProducerRecord($topic, $payload)",
            format
        )

        headersOf(result).forEach { (key, value) ->
            statement(lines, "$name.headers().add(${quoted(key, format)}, ${bytesOf(quoted(value, format), format)})", format)
        }
    }

    private fun stampCorrelation(lines: Lines, record: String, cid: String, result: AsyncApiCallResult, format: OutputFormat) {
        val header = result.getCorrelationHeader() ?: return
        statement(lines, "$record.headers().add(${quoted(header, format)}, ${bytesOf(cid, format)})", format)
    }

    private fun awaitReply(
        lines: Lines,
        consumer: String,
        variable: String,
        cid: String,
        deadline: String,
        timeout: Long,
        result: AsyncApiCallResult,
        format: OutputFormat
    ) {
        val header = result.getCorrelationHeader()

        when {
            format.isJava() -> lines.add("long $deadline = System.currentTimeMillis() + $timeout;")
            format.isKotlin() -> lines.add("val $deadline = System.currentTimeMillis() + $timeout")
        }

        lines.add("while ($variable == null && System.currentTimeMillis() < $deadline) {")
        lines.indented {
            val loop = if (format.isJava()) {
                "for (org.apache.kafka.clients.consumer.ConsumerRecord<String,String> r : " +
                        "$consumer.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {"
            } else {
                "for (r in $consumer.poll(java.time.Duration.ofMillis($DEFAULT_POLL_MS))) {"
            }
            lines.add(loop)
            lines.indented {
                if (header == null) {
                    //no correlation declared, so the first thing on the reply topic is taken
                    statement(lines, "$variable = r.value()", format)
                } else {
                    val readHeader = "r.headers().lastHeader(${quoted(header, format)})"
                    when {
                        format.isJava() -> lines.add("org.apache.kafka.common.header.Header h = $readHeader;")
                        format.isKotlin() -> lines.add("val h = $readHeader")
                    }
                    lines.add("if (h != null && $cid.equals(new String(h.value(), java.nio.charset.StandardCharsets.UTF_8))) {"
                        .let { if (format.isKotlin()) "if (h != null && $cid == String(h.value(), java.nio.charset.StandardCharsets.UTF_8)) {" else it })
                    lines.indented { statement(lines, "$variable = r.value()", format) }
                    lines.add("}")
                }
            }
            lines.add("}")
        }
        lines.add("}")
    }

    private fun headersOf(result: AsyncApiCallResult): Map<String, String> {
        val json = result.getHeadersAsJson() ?: return mapOf()
        return try {
            val node = mapper.readTree(json)
            node.fieldNames().asSequence().associateWith { node.get(it).asText() }
        } catch (e: Exception) {
            mapOf()
        }
    }

    private fun declareReply(lines: Lines, variable: String, format: OutputFormat) {
        when {
            format.isJava() -> lines.add("String $variable = null;")
            format.isKotlin() -> lines.add("var $variable: String? = null")
        }
    }

    private fun newInstance(
        lines: Lines,
        name: String,
        javaType: String,
        javaCtor: String,
        kotlinCtor: String,
        format: OutputFormat
    ) {
        when {
            format.isJava() -> lines.add("$javaType $name = new $javaCtor;")
            format.isKotlin() -> lines.add("val $name = $kotlinCtor")
        }
    }

    private fun assign(lines: Lines, name: String, expr: String, javaType: String, format: OutputFormat) {
        when {
            format.isJava() -> lines.add("$javaType $name = $expr;")
            format.isKotlin() -> lines.add("val $name = $expr")
        }
    }

    private fun statement(lines: Lines, code: String, format: OutputFormat) {
        lines.add(code)
        if (format.isJava()) {
            lines.append(";")
        }
    }

    private fun close(lines: Lines, name: String, format: OutputFormat) =
        statement(lines, "$name.close()", format)

    private fun bytesOf(expr: String, format: OutputFormat) =
        "$expr.toByteArray(java.nio.charset.StandardCharsets.UTF_8)"
            .let { if (format.isJava()) "$expr.getBytes(java.nio.charset.StandardCharsets.UTF_8)" else it }

    private fun nullLiteral(format: OutputFormat) = "null"

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
