package org.evomaster.core.output.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.core.EMConfig
import org.evomaster.core.output.asyncapi.KafkaTestClientEmitter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import java.nio.file.Path

/**
 * Writes a test for an AsyncAPI service.
 *
 * For Kafka, the lines that publish and await a reply are written here from the contract. For
 * other transports, the driver renders them during the search and they are copied as they are,
 * as done for RPC. Either way, the test needs no EvoMaster driver at run time.
 */
class AsyncApiTestCaseWriter : ApiTestCaseWriter {

    companion object {
        private val replyMapper = ObjectMapper()

        /**
         * Tolerance when comparing doubles, as the same value can differ slightly once
         * serialized to text.
         */
        private const val DELTA = 0.001
    }

    constructor() : super()

    /**
     * ONLY for tests
     */
    constructor(config: EMConfig) : super() {
        this.config = config
    }

    /**
     * The Kafka servers this suite publishes to, from the server's name (its key under `servers`
     * in the AsyncAPI document, eg "kafka") to its declared `host` (eg "localhost:9092"), used
     * when the driver does not give an address.
     *
     * Filled in when the class members are written, and read when the init statements are,
     * which the suite writer does afterwards.
     */
    private val declaredHostByServerName = LinkedHashMap<String, String>()

    /**
     * What the parsed reply is called in a generated test, before the index of the reply it
     * was parsed from.
     */
    private val bodyVariablePrefix = "body_"

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        sqlInsertionVars: MutableList<Pair<String, String>>,
        mongoInsertionVars: MutableList<Pair<String, String>>,
        redisInsertionVars: MutableList<Pair<String, String>>,
        dynamoDbInsertionVars: MutableList<Pair<String, String>>,
        neo4jInsertionVars: MutableList<Pair<String, String>>,
        testCaseName: String,
        testSuitePath: Path?
    ) {
        if (ind.individual !is AsyncApiIndividual) {
            return
        }

        //TODO no database handling yet for AsyncAPI

        ind.evaluatedMainActions().forEachIndexed { index, evaluated ->
            lines.addEmpty()
            addActionLines(
                evaluated.action,
                index,
                testCaseName,
                lines,
                evaluated.result,
                testSuitePath,
                baseUrlOfSut
            )
        }
    }

    override fun addActionLinesPerType(
        action: Action,
        index: Int,
        testCaseName: String,
        lines: Lines,
        result: ActionResult,
        testSuitePath: Path?,
        baseUrlOfSut: String
    ) {
        val asyncApiAction = action as? AsyncApiAction
            ?: throw IllegalArgumentException("Not an AsyncAPI action: ${action::class.java.simpleName}")
        val asyncApiResult = result as? AsyncApiCallResult
            ?: throw IllegalArgumentException("Not an AsyncAPI result: ${result::class.java.simpleName}")

        lines.addSingleCommentLine("${asyncApiAction.operationId}: ${asyncApiResult.getOutcome() ?: "not executed"}")

        val script = asyncApiResult.getTestScript()
        val replyVariable = asyncApiResult.getReplyVariableName()

        when {
            //rendered by the driver, for transports not written from the contract
            script.any { it.isNotBlank() } -> script.forEach { lines.add(it) }

            replyVariable != null && KafkaTestClientEmitter.canEmit(asyncApiResult) ->
                KafkaTestClientEmitter.emit(
                    lines, asyncApiResult, replyVariable, serverAddressExpression(asyncApiResult), format
                )

            else -> {
                lines.addSingleCommentLine(
                    "No lines could be written to publish this message: the transport is not one"
                )
                lines.addSingleCommentLine(
                    "written from the contract, and the driver rendered none. See"
                )
                lines.addSingleCommentLine(
                    "SutController.executeAsyncApiAction for what a driver can fill in."
                )
                return
            }
        }

        addReplyAssertions(lines, asyncApiResult)
    }

    /**
     * What the core can say about the reply, on top of whatever the driver's own lines assert.
     */
    private fun addReplyAssertions(lines: Lines, asyncApiResult: AsyncApiCallResult) {

        if (asyncApiResult.getOutcome() != AsyncApiOutcome.REPLIED) {
            return
        }

        val replyVariable = asyncApiResult.getReplyVariableName() ?: return

        lines.add("assertNotNull($replyVariable)")
        lines.appendSemicolon()

        asyncApiResult.getReplyMessage()?.let {
            lines.addSingleCommentLine("recognised as the declared message '$it'")
        }

        writeReplyAssertions(lines, asyncApiResult.getReplyPayload(), replyVariable, bodyVariableFor(replyVariable))
    }

    /**
     * Regression assertions on the reply a message received, from the reply seen during the
     * search, as done for a REST response body. The reply is text, so they are made on the JSON
     * tree it is parsed into.
     *
     * @param payload       the reply seen during the search
     * @param replyVariable the variable holding the reply in the test
     * @param bodyVariable  the variable to hold the parsed reply, if one is needed
     */
    internal fun writeReplyAssertions(lines: Lines, payload: String?, replyVariable: String, bodyVariable: String) {

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
            replyMapper.readTree(payload)
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

        writeReplyParsing(lines, replyVariable, bodyVariable)
        onReplyNode(lines, tree, bodyVariable, "")
    }

    private fun writeReplyParsing(lines: Lines, replyVariable: String, bodyVariable: String) {
        val read = "com.fasterxml.jackson.databind.ObjectMapper().readTree($replyVariable)"
        when {
            format.isJava() -> lines.add("com.fasterxml.jackson.databind.JsonNode $bodyVariable = new $read;")
            format.isKotlin() -> lines.add("val $bodyVariable = $read")
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    private fun onReplyNode(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {
        when {
            node.isObject -> onReplyObject(lines, node, nodeExpression, jsonPath)
            node.isArray -> onReplyArray(lines, node, nodeExpression, jsonPath)
            else -> onReplyValue(lines, node, nodeExpression, jsonPath)
        }
    }

    private fun onReplyObject(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {

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

            onReplyNode(lines, node.get(name), childExpression(nodeExpression, name), childJsonPath)
        }
    }

    private fun onReplyArray(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {

        writeAssertEquals(lines, node.size().toString(), "$nodeExpression.size()")

        val limit = if (config.maxAssertionForDataInCollection >= 0) {
            config.maxAssertionForDataInCollection
        } else {
            node.size()
        }

        node.take(limit).forEachIndexed { i, child ->
            onReplyNode(lines, child, "$nodeExpression.get($i)", "$jsonPath[$i]")
        }

        if (node.size() > limit) {
            lines.addSingleCommentLine(
                "the remaining ${node.size() - limit} elements are not asserted on;" +
                        " the limit is maxAssertionForDataInCollection"
            )
        }
    }

    private fun onReplyValue(lines: Lines, node: JsonNode, nodeExpression: String, jsonPath: String) {
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

    /**
     * A message that could not be published is recorded during the search, so a generated test
     * does not expect an exception.
     */
    override fun shouldFailIfExceptionNotThrown(result: ActionResult) = false

    /**
     * A variable per Kafka server the suite publishes to, when there is a driver to ask where it
     * is. See [org.evomaster.client.java.controller.SutHandler.getAsyncApiServerAddress].
     */
    override fun addExtraClassMembers(lines: Lines, solution: Solution<*>) {

        val kafkaResults = KafkaTestClientEmitter.resultsIn(solution)

        //the same writer is used for every suite of a run, so clear what the previous one left
        declaredHostByServerName.clear()

        if (kafkaResults.isEmpty()) {
            return
        }

        if (!suiteHasDriver()) {
            return
        }

        kafkaResults.forEach { kafkaResult ->
            val name = kafkaResult.getServerName()
            if (name.isNullOrBlank()) {
                return@forEach
            }
            //names like "a-b" and "a.b" map to the same variable: only the first gets one
            if (declaredHostByServerName.keys.any { it != name && serverVariableFor(it) == serverVariableFor(name) }) {
                return@forEach
            }
            declaredHostByServerName.putIfAbsent(name, kafkaResult.getServerHost() ?: "")
        }

        declaredHostByServerName.keys.forEach { name ->
            when {
                format.isJava() -> lines.add("private static String ${serverVariableFor(name)};")
                format.isKotlin() -> lines.add("private lateinit var ${serverVariableFor(name)}: String")
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
        }
        lines.addEmpty()
    }

    /**
     * Ask the driver where each server is, falling back to the address in the document.
     */
    override fun addExtraInitStatement(lines: Lines) {

        declaredHostByServerName.forEach { (name, declaredHost) ->
            val variable = serverVariableFor(name)
            val askDriver = "${TestSuiteWriter.controller}.getAsyncApiServerAddress(" +
                    TestWriterUtils.quotedEscaped(name, format) + ")"
            val fallback = TestWriterUtils.quotedEscaped(declaredHost, format)

            when {
                //two statements, so the driver is asked only once
                format.isJava() -> {
                    lines.add("$variable = $askDriver;")
                    lines.add("if ($variable == null) {")
                    lines.indented {
                        lines.add("$variable = $fallback;")
                    }
                    lines.add("}")
                }
                format.isKotlin() -> lines.add("$variable = $askDriver ?: $fallback")
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
        }
    }

    /**
     * Where to publish: the variable filled in from the driver, or the document's address when
     * there is no driver.
     */
    private fun serverAddressExpression(asyncApiResult: AsyncApiCallResult): String {

        val name = asyncApiResult.getServerName()

        if (suiteHasDriver() && name != null && declaredHostByServerName.containsKey(name)) {
            return serverVariableFor(name)
        }

        return TestWriterUtils.quotedEscaped(asyncApiResult.getServerHost() ?: "", format)
    }

    /**
     * Name of the variable holding the parsed reply.
     */
    private fun bodyVariableFor(replyVariable: String) =
        bodyVariablePrefix + replyVariable.substringAfterLast('_')

    private fun serverVariableFor(serverName: String) =
        KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX + TestWriterUtils.safeVariableName(serverName)

    /**
     * Whether the generated suite has a driver to ask where a server is.
     */
    private fun suiteHasDriver() = (!config.blackBox || config.bbExperiments) && format.isJavaOrKotlin()

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {

        val actions = test.test.evaluatedMainActions()

        //as REST does, with the outcome in place of the status code
        if (actions.isNotEmpty()) {
            lines.addBlockCommentLine("Calls:")
            actions.forEachIndexed { index, evaluated ->
                val outcome = (evaluated.result as? AsyncApiCallResult)?.getOutcome() ?: "not executed"
                val prefix = if (actions.size == 1) "" else "${index + 1} - "
                lines.addBlockCommentLine("$prefix($outcome) ${evaluated.action.getName()}")
            }
        }
    }
}
