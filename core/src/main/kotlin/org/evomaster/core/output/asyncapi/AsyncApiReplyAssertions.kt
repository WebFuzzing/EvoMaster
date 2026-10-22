package org.evomaster.core.output.asyncapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestWriterUtils

/**
 * Writes regression assertions on the reply a message received, from the reply seen during the
 * search, as the REST writer does on a response body.
 *
 * The reply is text, so the assertions are made on the JSON tree it is parsed into.
 *
 * @param isFieldToSkip     whether a field's value must not be asserted on, eg because it changes
 *                          between runs. The REST writer's rule is passed here.
 * @param isSuitableToPrint whether a value can be asserted on without making the test flaky.
 *                          The REST writer's rule is passed here.
 */
class AsyncApiReplyAssertions(
    private val config: EMConfig,
    private val format: OutputFormat,
    private val isFieldToSkip: (String) -> Boolean,
    private val isSuitableToPrint: (String) -> Boolean
) {

    companion object {
        private val mapper = ObjectMapper()

        /**
         * Tolerance when comparing doubles, as the same value can differ slightly once
         * serialized to text.
         */
        private const val DELTA = 0.001
    }

    init {
        if (!format.isJavaOrKotlin()) {
            throw IllegalArgumentException("Reply assertions are written only for Java and Kotlin, not $format")
        }
    }

    /**
     * @param payload      the reply seen during the search
     * @param variable     the variable holding the reply in the test
     * @param bodyVariable the variable to hold the parsed reply, if one is needed
     */
    fun emit(lines: Lines, payload: String?, variable: String, bodyVariable: String) {

        if (!config.enableBasicAssertions) {
            return
        }

        if (payload.isNullOrBlank()) {
            //an acknowledgement, or a transport that answers in metadata
            assertion(lines, "$variable == null || $variable.isEmpty()")
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
            //not JSON, so the reply can only be checked as text
            if (isSuitableToPrint(payload)) {
                contains(lines, variable, payload.trim())
            } else {
                lines.addSingleCommentLine(
                    "the reply is not JSON, and its text is not asserted on as it might differ between runs"
                )
            }
            return
        }

        parse(lines, variable, bodyVariable)
        onNode(lines, tree, bodyVariable, "")
    }

    private fun parse(lines: Lines, variable: String, bodyVariable: String) {
        val read = "com.fasterxml.jackson.databind.ObjectMapper().readTree($variable)"
        when {
            format.isJava() -> lines.add("com.fasterxml.jackson.databind.JsonNode $bodyVariable = new $read;")
            format.isKotlin() -> lines.add("val $bodyVariable = $read")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun onNode(lines: Lines, node: JsonNode, path: String, fieldPath: String) {
        when {
            node.isObject -> onObject(lines, node, path, fieldPath)
            node.isArray -> onArray(lines, node, path, fieldPath)
            else -> onValue(lines, node, path, fieldPath)
        }
    }

    private fun onObject(lines: Lines, node: JsonNode, path: String, fieldPath: String) {

        if (node.isEmpty) {
            assertion(lines, "$path.isEmpty()")
            return
        }

        node.fieldNames().forEach { name ->

            val here = if (fieldPath.isEmpty()) name else "$fieldPath.$name"

            //the field being there is stable even when its value is not
            has(lines, path, name)

            if (isFieldToSkip(name)) {
                lines.addSingleCommentLine("$here is not asserted on, as its value changes between runs")
                return@forEach
            }

            onNode(lines, node.get(name), child(path, name), here)
        }
    }

    private fun onArray(lines: Lines, node: JsonNode, path: String, fieldPath: String) {

        equals(lines, node.size().toString(), "$path.size()")

        val limit = if (config.maxAssertionForDataInCollection >= 0) {
            config.maxAssertionForDataInCollection
        } else {
            node.size()
        }

        node.take(limit).forEachIndexed { i, child ->
            onNode(lines, child, "$path.get($i)", "$fieldPath[$i]")
        }

        if (node.size() > limit) {
            lines.addSingleCommentLine(
                "the remaining ${node.size() - limit} elements are not asserted on;" +
                        " the limit is maxAssertionForDataInCollection"
            )
        }
    }

    private fun onValue(lines: Lines, node: JsonNode, path: String, fieldPath: String) {
        when {
            node.isNull -> assertion(lines, "$path.isNull()")

            node.isBoolean -> equals(lines, node.asBoolean().toString(), "$path.asBoolean()")

            node.isIntegralNumber -> {
                val v = node.asLong()
                val suffix = if (format.isJava() && (v > Int.MAX_VALUE || v < Int.MIN_VALUE)) "L" else ""
                equals(lines, "$v$suffix", "$path.asLong()")
            }

            node.isNumber -> {
                val v = node.asDouble()
                //out of range it parses as an infinity, which has no literal
                if (v.isFinite()) {
                    delta(lines, v.toString(), "$path.asDouble()")
                } else {
                    lines.addSingleCommentLine(
                        "$fieldPath is not asserted on, as it is outside the range of a double"
                    )
                }
            }

            node.isTextual -> {
                val text = node.asText()
                if (isSuitableToPrint(text)) {
                    equals(lines, TestWriterUtils.quotedEscaped(text, format), "$path.asText()")
                } else {
                    lines.addSingleCommentLine(
                        "$fieldPath is not asserted on, as its value might differ between runs"
                    )
                }
            }
        }
    }

    private fun has(lines: Lines, path: String, field: String) =
        assertion(lines, "$path.has(${TestWriterUtils.quotedEscaped(field, format)})")

    private fun child(path: String, name: String) =
        "$path.get(${TestWriterUtils.quotedEscaped(name, format)})"

    /**
     * [expression] is valid in both Java and Kotlin.
     */
    private fun assertion(lines: Lines, expression: String) {
        when {
            format.isJava() -> lines.add("assertTrue($expression);")
            format.isKotlin() -> lines.add("assertTrue($expression)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun equals(lines: Lines, expected: String, actual: String) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun delta(lines: Lines, expected: String, actual: String) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual, $DELTA);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual, $DELTA)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun contains(lines: Lines, variable: String, text: String) {
        val expected = TestWriterUtils.quotedEscaped(text, format)
        when {
            format.isJava() -> lines.add("assertTrue($variable.contains($expected));")
            format.isKotlin() -> lines.add("assertTrue($variable!!.contains($expected))")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }
}
