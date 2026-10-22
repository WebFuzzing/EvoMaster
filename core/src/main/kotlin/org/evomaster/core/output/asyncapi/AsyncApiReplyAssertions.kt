package org.evomaster.core.output.asyncapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils

/**
 * Writes assertions over the reply a message drew, from the reply the search actually saw.
 *
 * These are regression assertions, the same kind the REST writer makes on a response body: they
 * say what came back, not what should, and they honour the same rules about what is too unstable
 * to assert on, because a flaky test is flaky whatever the protocol.
 *
 * The reply is text rather than a response object, so there is no fluent client to hang them
 * off. They are made against the tree it parses into.
 */
object AsyncApiReplyAssertions {

    private val mapper = ObjectMapper()

    /**
     * The fields REST skips, for the same reason: their value usually differs on the next run.
     */
    private val ALWAYS_SKIPPED = listOf("id", "timestamp", "self")

    /**
     * Write what can be said about [payload], into [variable] as the test holds it.
     *
     * @param bodyVariable what the parsed reply is called, when one is needed
     */
    fun emit(
        lines: Lines,
        payload: String?,
        variable: String,
        bodyVariable: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        if (!config.enableBasicAssertions) {
            return
        }

        if (payload.isNullOrBlank()) {
            //an acknowledgement, or a transport that answers in metadata
            val java = "$variable == null || $variable.isEmpty()"
            val kotlin = "$variable.isNullOrEmpty()"
            assertion(lines, kotlin, java, format)
            return
        }

        if (payload.length > config.maxResponseByteSize) {
            lines.addSingleCommentLine(
                "the reply was larger than the threshold of ${config.maxResponseByteSize} bytes," +
                        " so no assertion is made on it"
            )
            return
        }

        val tree = try {
            mapper.readTree(payload)
        } catch (e: Exception) {
            null
        }

        if (tree == null || tree.isMissingNode) {
            //not JSON: all that can be said is what it contained
            if (printable(payload)) {
                contains(lines, variable, payload.trim(), format)
            } else {
                lines.addSingleCommentLine(
                    "the reply is not asserted on, as its value is not stable between runs"
                )
            }
            return
        }

        parse(lines, variable, bodyVariable, format)
        onNode(lines, tree, bodyVariable, "", config, format)
    }

    private fun parse(lines: Lines, variable: String, bodyVariable: String, format: OutputFormat) {
        val read = "com.fasterxml.jackson.databind.ObjectMapper().readTree($variable)"
        when {
            format.isJava() -> lines.add(
                "com.fasterxml.jackson.databind.JsonNode $bodyVariable = " +
                        "new com.fasterxml.jackson.databind.ObjectMapper().readTree($variable);"
            )
            format.isKotlin() -> lines.add("val $bodyVariable = $read")
        }
    }

    /**
     * Everything that can be said about one node, and about what is under it.
     */
    private fun onNode(
        lines: Lines,
        node: JsonNode,
        path: String,
        fieldPath: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        when {
            node.isObject -> onObject(lines, node, path, fieldPath, config, format)
            node.isArray -> onArray(lines, node, path, fieldPath, config, format)
            else -> onValue(lines, node, path, fieldPath, config, format)
        }
    }

    private fun onObject(
        lines: Lines,
        node: JsonNode,
        path: String,
        fieldPath: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        if (node.isEmpty) {
            val java = "$path.isEmpty()"
            assertion(lines, if (format.isKotlin()) "$path.isEmpty" else java, java, format)
            return
        }

        node.fieldNames().asSequence().toList().forEach { name ->

            val child = node.get(name)
            val here = if (fieldPath.isEmpty()) name else "$fieldPath.$name"

            //that the field is there is stable, and is what tells one declared message from
            //another; only its value changes between runs
            has(lines, path, name, format)

            if (skip(name, config)) {
                lines.addSingleCommentLine("$here is not asserted on, as its value changes between runs")
                return@forEach
            }

            onNode(lines, child, child(path, name, format), here, config, format)
        }
    }

    private fun onArray(
        lines: Lines,
        node: JsonNode,
        path: String,
        fieldPath: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        equals(lines, node.size().toString(), "$path.size()", format)

        val limit = if (config.maxAssertionForDataInCollection >= 0) {
            config.maxAssertionForDataInCollection
        } else {
            node.size()
        }

        node.take(limit).forEachIndexed { i, child ->
            onNode(lines, child, index(path, i), "$fieldPath[$i]", config, format)
        }

        if (node.size() > limit) {
            lines.addSingleCommentLine(
                "the remaining ${node.size() - limit} elements are not asserted on;" +
                        " the limit is maxAssertionForDataInCollection"
            )
        }
    }

    private fun onValue(
        lines: Lines,
        node: JsonNode,
        path: String,
        fieldPath: String,
        config: EMConfig,
        format: OutputFormat
    ) {
        when {
            node.isNull -> {
                val java = "$path.isNull()"
                assertion(lines, if (format.isKotlin()) "$path.isNull" else java, java, format)
            }

            node.isBoolean -> equals(
                lines,
                node.asBoolean().toString(),
                value(path, "asBoolean()"),
                format
            )

            node.isIntegralNumber -> {
                val v = node.asLong()
                val suffix = if (format.isJava() && (v > Int.MAX_VALUE || v < Int.MIN_VALUE)) "L" else ""
                equals(lines, "$v$suffix", value(path, "asLong()"), format)
            }

            //within a tolerance, as REST does: the same computation lands a bit apart through text
            node.isNumber -> {
                val v = node.asDouble()
                //out of range it parses as an infinity, which no target language writes as one
                if (v.isFinite()) {
                    delta(lines, v.toString(), value(path, "asDouble()"), format)
                } else {
                    lines.addSingleCommentLine(
                        "$fieldPath is not asserted on, as it is outside the range of a double"
                    )
                }
            }

            node.isTextual -> {
                val text = node.asText()
                if (printable(text)) {
                    equals(lines, quoted(text, format), value(path, "asText()"), format)
                } else {
                    lines.addSingleCommentLine(
                        "$fieldPath is not asserted on, as its value is not stable between runs"
                    )
                }
            }
        }
    }

    /**
     * Whether a value can be asserted on without making the test flaky: REST's rule, shared
     * rather than restated, since a copy of it here had already drifted from it.
     */
    private fun printable(content: String) = TestWriterUtils.isSuitableToPrint(content)

    private fun skip(fieldName: String, config: EMConfig): Boolean {

        val field = fieldName.lowercase()

        val configured = config.fieldsToSkipInAssertions
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        return ALWAYS_SKIPPED.contains(field) || configured.contains(field)
    }

    private fun has(lines: Lines, path: String, field: String, format: OutputFormat) {
        val name = quoted(field, format)
        val expr = "$path.has($name)"
        assertion(lines, expr, expr, format)
    }

    //a field name is a literal too, and JSON allows ones holding a quote, or starting with '$',
    //which Kotlin would otherwise read as the start of a template
    private fun child(path: String, name: String, format: OutputFormat) =
        "$path.get(${quoted(name, format)})"

    private fun index(path: String, i: Int) = "$path.get($i)"

    private fun value(path: String, asType: String) = "$path.$asType"

    private fun assertion(lines: Lines, kotlin: String, java: String, format: OutputFormat) {
        when {
            format.isJava() -> lines.add("assertTrue($java);")
            format.isKotlin() -> lines.add("assertTrue($kotlin)")
        }
    }

    private fun equals(lines: Lines, expected: String, actual: String, format: OutputFormat) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual)")
        }
    }

    private fun delta(lines: Lines, expected: String, actual: String, format: OutputFormat) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual, 0.001);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual, 0.001)")
        }
    }

    private fun contains(lines: Lines, variable: String, text: String, format: OutputFormat) {
        val expected = quoted(text, format)
        when {
            format.isJava() -> lines.add("assertTrue($variable.contains($expected));")
            format.isKotlin() -> lines.add("assertTrue($variable!!.contains($expected))")
        }
    }

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
