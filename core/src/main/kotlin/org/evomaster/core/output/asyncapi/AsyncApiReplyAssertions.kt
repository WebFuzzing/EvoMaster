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
     * @param payload       the reply seen during the search
     * @param replyVariable the variable holding the reply in the test
     * @param bodyVariable  the variable to hold the parsed reply, if one is needed
     */
    fun emit(lines: Lines, payload: String?, replyVariable: String, bodyVariable: String) {

        if (!config.enableBasicAssertions) {
            return
        }

        if (payload.isNullOrBlank()) {
            //an acknowledgement, or a transport that answers in metadata
            writeAssertTrue(lines, "$replyVariable == null || $replyVariable.isEmpty()")
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
                writeAssertContains(lines, replyVariable, payload.trim())
            } else {
                lines.addSingleCommentLine(
                    "the reply is not JSON, and its text is not asserted on as it might differ between runs"
                )
            }
            return
        }

        parse(lines, replyVariable, bodyVariable)
        onNode(lines, tree, bodyVariable, "")
    }

    private fun parse(lines: Lines, replyVariable: String, bodyVariable: String) {
        val read = "com.fasterxml.jackson.databind.ObjectMapper().readTree($replyVariable)"
        when {
            format.isJava() -> lines.add("com.fasterxml.jackson.databind.JsonNode $bodyVariable = new $read;")
            format.isKotlin() -> lines.add("val $bodyVariable = $read")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun onNode(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {
        when {
            node.isObject -> onObject(lines, node, nodeExpression, jsonPath)
            node.isArray -> onArray(lines, node, nodeExpression, jsonPath)
            else -> onValue(lines, node, nodeExpression, jsonPath)
        }
    }

    private fun onObject(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {

        if (node.isEmpty) {
            writeAssertTrue(lines, "$nodeExpression.isEmpty()")
            return
        }

        node.fieldNames().forEach { name ->

            val childJsonPath = if (jsonPath.isEmpty()) name else "$jsonPath.$name"

            //the field being there is stable even when its value is not
            writeAssertHasField(lines, nodeExpression, name)

            if (isFieldToSkip(name)) {
                lines.addSingleCommentLine("$childJsonPath is not asserted on, as its value changes between runs")
                return@forEach
            }

            onNode(lines, node.get(name), childExpression(nodeExpression, name), childJsonPath)
        }
    }

    private fun onArray(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {

        writeAssertEquals(lines, node.size().toString(), "$nodeExpression.size()")

        val limit = if (config.maxAssertionForDataInCollection >= 0) {
            config.maxAssertionForDataInCollection
        } else {
            node.size()
        }

        node.take(limit).forEachIndexed { i, child ->
            onNode(lines, child, "$nodeExpression.get($i)", "$jsonPath[$i]")
        }

        if (node.size() > limit) {
            lines.addSingleCommentLine(
                "the remaining ${node.size() - limit} elements are not asserted on;" +
                        " the limit is maxAssertionForDataInCollection"
            )
        }
    }

    private fun onValue(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {
        when {
            node.isNull -> writeAssertTrue(lines, "$nodeExpression.isNull()")

            node.isBoolean -> writeAssertEquals(lines, node.asBoolean().toString(), "$nodeExpression.asBoolean()")

            node.isIntegralNumber -> {
                val v = node.asLong()
                val suffix = if (format.isJava() && (v > Int.MAX_VALUE || v < Int.MIN_VALUE)) "L" else ""
                writeAssertEquals(lines, "$v$suffix", "$nodeExpression.asLong()")
            }

            node.isNumber -> {
                val v = node.asDouble()
                //out of range it parses as an infinity, which has no literal
                if (v.isFinite()) {
                    writeAssertEqualsWithDelta(lines, v.toString(), "$nodeExpression.asDouble()")
                } else {
                    lines.addSingleCommentLine(
                        "$jsonPath is not asserted on, as it is outside the range of a double"
                    )
                }
            }

            node.isTextual -> {
                val text = node.asText()
                if (isSuitableToPrint(text)) {
                    writeAssertEquals(lines, TestWriterUtils.quotedEscaped(text, format), "$nodeExpression.asText()")
                } else {
                    lines.addSingleCommentLine(
                        "$jsonPath is not asserted on, as its value might differ between runs"
                    )
                }
            }
        }
    }

    private fun writeAssertHasField(lines: Lines, nodeExpression: String, field: String) =
        writeAssertTrue(lines, "$nodeExpression.has(${TestWriterUtils.quotedEscaped(field, format)})")

    private fun childExpression(nodeExpression: String, name: String) =
        "$nodeExpression.get(${TestWriterUtils.quotedEscaped(name, format)})"

    /**
     * [expression] is valid in both Java and Kotlin.
     */
    private fun writeAssertTrue(lines: Lines, expression: String) {
        when {
            format.isJava() -> lines.add("assertTrue($expression);")
            format.isKotlin() -> lines.add("assertTrue($expression)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun writeAssertEquals(lines: Lines, expected: String, actual: String) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun writeAssertEqualsWithDelta(lines: Lines, expected: String, actual: String) {
        when {
            format.isJava() -> lines.add("assertEquals($expected, $actual, $DELTA);")
            format.isKotlin() -> lines.add("assertEquals($expected, $actual, $DELTA)")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun writeAssertContains(lines: Lines, replyVariable: String, text: String) {
        val expected = TestWriterUtils.quotedEscaped(text, format)
        when {
            format.isJava() -> lines.add("assertTrue($replyVariable.contains($expected));")
            format.isKotlin() -> lines.add("assertTrue($replyVariable!!.contains($expected))")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }
}
