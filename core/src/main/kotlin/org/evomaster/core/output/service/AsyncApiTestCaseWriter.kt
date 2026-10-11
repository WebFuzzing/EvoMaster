package org.evomaster.core.output.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import org.evomaster.core.logging.LoggingUtil
import org.slf4j.LoggerFactory
import org.evomaster.core.EMConfig
import org.evomaster.core.output.asyncapi.KafkaTestClientEmitter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestWriterUtils
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.problem.asyncapi.service.AsyncApiSampler
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.action.ActionResult
import org.slf4j.Logger
import java.nio.file.Path

/**
 * Writes a test for an AsyncAPI service.
 *
 * For Kafka, the lines that publish and await a reply are written here from the contract. For
 * other transports, the driver renders them during the search and they are copied as they are,
 * as done for RPC. Publishing messages does not go through the EvoMaster driver. White-box
 * and black-box experiment suites still use the driver for setup and server-address resolution.
 */
class AsyncApiTestCaseWriter : ApiTestCaseWriter {

    companion object {
        private val replyMapper = ObjectMapper()

        /** Payload types whose recorded text represents their wire content. */
        private val TEXT_PAYLOAD_TYPES = setOf(String::class.java.name, ByteArray::class.java.name)
        private val log: Logger = LoggerFactory.getLogger(AsyncApiTestCaseWriter::class.java)

        /**
         * Tolerance when comparing doubles, as the same value can differ slightly once
         * serialized to text.
         */
        private const val DELTA = 0.001
    }

    @Inject
    private lateinit var sampler: AsyncApiSampler

    /**
     * The Kafka servers the schema's channels publish to, from the server's name (its key under
     * `servers`, eg "kafka") to its declared `host` (eg "localhost:9092"), used when the driver
     * does not give an address. A channel publishes to its first server, as in the search.
     */
    private val declaredHostByServerName: Map<String, String> by lazy {
        val document = sampler.document
        val hosts = LinkedHashMap<String, String>()
        document.channels.values
            .mapNotNull { document.serversOf(it).firstOrNull() }
            .filter { KafkaTestClientEmitter.isKafka(it.protocol) && !it.name.isNullOrBlank() }
            .forEach { server ->
                //names like "a-b" and "a.b" map to the same variable: only the first gets one
                if (hosts.keys.none { serverVariableFor(it) == serverVariableFor(server.name) }) {
                    hosts[server.name] = server.host ?: ""
                }
            }
        hosts
    }

    /**
     * Prefix of the variable holding a parsed reply, followed by the index of its action.
     */
    private val bodyVariablePrefix = "body_"

    constructor() : super()

    /**
     * ONLY for tests
     */
    constructor(config: EMConfig) : super() {
        this.config = config
    }

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
        val mocking = config.isEnabledExternalServiceMocking()
        val mocked = if (mocking) mockedServicesOf(ind) else listOf()

        ind.evaluatedMainActions().forEachIndexed { index, evaluated ->
            lines.addEmpty()
            if (mocking) {
                /*
                    As the search does before each message, so a stub set up for one message never
                    answers a call made for the next. The suite writer does it before each test.
                    This also clears what the instances were asked, for the wait below.
                 */
                if (index > 0) {
                    resetMockedServices(lines, mocked)
                }
                addExternalServiceStubs(lines, evaluated.action)
            }
            addActionLines(
                evaluated.action,
                index,
                testCaseName,
                lines,
                evaluated.result,
                testSuitePath,
                baseUrlOfSut
            )
            //the calls the service makes say when it is done better than a fixed wait does
            val awaited = mocking && addServedRequestsWait(lines, evaluated.action, evaluated.result)
            if (!awaited) {
                addSettleWait(lines, evaluated.result)
            }
        }
    }

    /**
     * The WireMock instances some message of [ind] has a stub on, once each.
     */
    private fun mockedServicesOf(ind: EvaluatedIndividual<*>): List<HttpWsExternalService> =
        ind.individual.seeActions(ActionFilter.ONLY_EXTERNAL_SERVICE)
            .filterIsInstance<HttpExternalServiceAction>()
            .filter { it.active }
            .map { it.externalService }
            .distinctBy { it.getSignature() }

    /**
     * Put [services] back to their default answer, and forget what they were asked, as the
     * suite writer does before each test.
     *
     * Only for Java and Kotlin, the formats the suite writer starts WireMock in.
     */
    private fun resetMockedServices(lines: Lines, services: List<HttpWsExternalService>) {

        if (services.isEmpty() || !format.isJavaOrKotlin()) {
            return
        }

        services.forEach { service ->
            lines.addStatement("${TestWriterUtils.getWireMockVariableName(service)}.resetAll()")
            TestWriterUtils.handleDefaultStubForAsJavaOrKotlin(lines, service, format)
            lines.appendSemicolon()
        }
    }

    /**
     * After [action], wait until each stub the search saw the service use while handling the
     * message was called, or for as long as the search waited on the message, whichever ends
     * first. Counted per stub, so that a call the default answer takes does not end the wait.
     *
     * Otherwise a late call could reach the instances after they were reset for the next
     * message, or the test ended, and get another answer than the search gave it.
     *
     * Only for Java and Kotlin, the formats the suite writer starts WireMock in.
     *
     * @return whether a wait was written
     */
    private fun addServedRequestsWait(lines: Lines, action: Action, result: ActionResult): Boolean {

        //nothing to wait for if the message never went out
        val res = result as? AsyncApiCallResult ?: return false
        if (!format.isJavaOrKotlin() || !isPublished(res)) {
            return false
        }

        val group = action.parent as? EnterpriseActionGroup<*> ?: return false

        //the request pattern as handleExternalServiceActions registers the stub
        val count = if (format.isJava()) "getCount()" else "count"
        val pending = group.getExternalServiceActions()
            .filterIsInstance<HttpExternalServiceAction>()
            .filter { it.active && it.used }
            .map {
                val instance = TestWriterUtils.getWireMockVariableName(it.externalService)
                val url = TestWriterUtils.quotedEscaped(it.request.url, format)
                val pattern = "${it.request.method.lowercase()}RequestedFor(urlEqualTo($url))"
                val served = "$instance.countRequestsMatching($pattern.build()).$count > 0"
                if (format.isJava()) "() -> $served"
                else "java.util.function.BooleanSupplier { $served }"
            }
            .distinct()
            .joinToString(", ")

        if (pending.isEmpty()) {
            return false
        }

        lines.addStatement("org.evomaster.test.utils.EMTestUtils.awaitAll(${completionTimeoutMs()}L, $pending)")

        return true
    }

    /**
     * Set up what the WireMock instances answer while the service handles [action], as the
     * search did. The suite writer declares, starts and resets them before each test, and
     * [handleActionCalls] resets them between messages.
     *
     * Only for Java and Kotlin, the formats the suite writer starts WireMock in.
     */
    private fun addExternalServiceStubs(lines: Lines, action: Action) {

        val group = action.parent as? EnterpriseActionGroup<*>
            ?: throw IllegalStateException(
                "The parent of an AsyncAPI action should be an EnterpriseActionGroup, but it is" +
                        " ${action.parent?.javaClass?.simpleName}"
            )

        val stubs = group.getExternalServiceActions()
            .filterIsInstance<HttpExternalServiceAction>()
            .filter { it.active }

        if (stubs.isEmpty()) {
            return
        }

        if (!format.isJavaOrKotlin()) {
            LoggingUtil.uniqueWarn(
                log,
                "In mocking of external services, only Java and Kotlin are supported, not $format"
            )
            return
        }

        handleExternalServiceActions(lines, stubs)
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
            script.isNotEmpty() -> script.forEach { lines.add(it) }

            //isEmitted already requires a reply variable
            KafkaTestClientEmitter.isEmitted(asyncApiResult) -> {
                val broker = serverAddressExpression(asyncApiResult)
                val observer = "observer_$index"
                KafkaTestClientEmitter.emitObserve(lines, asyncApiResult, observer, broker, format)
                KafkaTestClientEmitter.emit(lines, asyncApiResult, replyVariable!!, broker, format, observer)
                KafkaTestClientEmitter.emitAwait(
                    lines, asyncApiResult, observer, "emitted_$index", completionTimeoutMs(), format
                )
            }

            asyncApiResult.getOutcome() == AsyncApiOutcome.PUBLISH_FAILED -> {
                lines.addSingleCommentLine("The driver could not publish this message while the search ran")
                return
            }

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

        addReplyAssertions(lines, asyncApiResult, index)
        if (KafkaTestClientEmitter.isEmitted(asyncApiResult)) {
            writeEmittedAssertions(lines, asyncApiResult, "emitted_$index", "emitted_${index}_", "emittedBody_${index}_")
        }
    }

    /**
     * What the core can say about the reply, on top of whatever the driver's own lines assert.
     */
    private fun addReplyAssertions(lines: Lines, asyncApiResult: AsyncApiCallResult, index: Int) {

        if (asyncApiResult.getOutcome() != AsyncApiOutcome.REPLIED) {
            return
        }

        val replyVariable = asyncApiResult.getReplyVariableName() ?: return

        lines.addStatement("assertNotNull($replyVariable)")

        asyncApiResult.getReplyMessage()?.let {
            lines.addSingleCommentLine("recognised as the declared message '$it'")
        }

        writeReplyAssertions(lines, asyncApiResult.getReplyPayload(), replyVariable, bodyVariablePrefix + index)
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

        //the assertions below are written for Java and Kotlin only
        if (!format.isJavaOrKotlin()) {
            throw IllegalArgumentException("Unsupported format: $format")
        }

        if (!config.enableBasicAssertions) {
            return
        }

        if (payload.isNullOrBlank()) {
            //an acknowledgement, or a transport that answers in metadata
            writeJUnitAssertTrue(lines, "$replyVariable == null || $replyVariable.isEmpty()")
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
                writeJUnitAssertContains(lines, replyVariable, payload.trim())
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
            writeJUnitAssertTrue(lines, "$nodeExpression.isEmpty()")
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

        writeJUnitAssertEquals(lines, node.size().toString(), "$nodeExpression.size()")

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
            node.isNull -> writeJUnitAssertTrue(lines, "$nodeExpression.isNull()")

            node.isBoolean -> writeJUnitAssertEquals(lines, node.asBoolean().toString(), "$nodeExpression.asBoolean()")

            node.isIntegralNumber -> {
                val v = node.asLong()
                val suffix = if (format.isJava() && (v > Int.MAX_VALUE || v < Int.MIN_VALUE)) "L" else ""
                writeJUnitAssertEquals(lines, "$v$suffix", "$nodeExpression.asLong()")
            }

            node.isNumber -> {
                val v = node.asDouble()
                //out of range it parses as an infinity, which has no literal
                if (v.isFinite()) {
                    writeJUnitAssertEqualsWithDelta(lines, v.toString(), "$nodeExpression.asDouble()", DELTA)
                } else {
                    lines.addSingleCommentLine(
                        "$jsonPath is not asserted on, as it is outside the range of a double"
                    )
                }
            }

            node.isTextual -> {
                val text = node.asText()
                if (isSuitableToPrint(text)) {
                    writeJUnitAssertEquals(lines, TestWriterUtils.quotedEscaped(text, format), "$nodeExpression.asText()")
                } else {
                    lines.addSingleCommentLine(
                        "$jsonPath is not asserted on, as its value might differ between runs"
                    )
                }
            }

            else -> LoggingUtil.uniqueWarn(log, "No reply assertion for JSON node type ${node.nodeType}")
        }
    }

    private fun writeAssertHasField(lines: Lines, nodeExpression: String, field: String) =
        writeJUnitAssertTrue(lines, "$nodeExpression.has(${TestWriterUtils.quotedEscaped(field, format)})")

    private fun childExpression(nodeExpression: String, name: String) =
        "$nodeExpression.get(${TestWriterUtils.quotedEscaped(name, format)})"

    /**
     * A message that could not be published is recorded during the search, so a generated test
     * does not expect an exception.
     */
    override fun shouldFailIfExceptionNotThrown(result: ActionResult) = false

    /**
     * A variable per Kafka server, when there is a driver to ask where it is. See
     * [org.evomaster.client.java.controller.SutHandler.getAsyncApiServerAddress].
     */
    override fun addExtraStaticVariables(lines: Lines) {

        if (!suiteHasDriver() || declaredHostByServerName.isEmpty()) {
            return
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

        if (!suiteHasDriver()) {
            return
        }

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

    /**
     * After a published message, wait for the window the search kept it open, when it did, so
     * that what the service does handling it does not land on the next message or test.
     *
     * The whole window, not what the search ended up waiting: an early end came from the
     * instrumentation, which a generated test has not got. Also after a reply, as the handler may
     * still be running. Not when the test waits for the calls to the mocked services instead.
     *
     * Only for Java and Kotlin: only a white-box run keeps a message open, and those are the
     * formats it writes.
     */
    private fun addSettleWait(lines: Lines, result: ActionResult) {

        val res = result as? AsyncApiCallResult ?: return

        if (!isPublished(res)
            || res.getSettledMs() == null
            || config.asyncApiSettleMs <= 0
            || !format.isJavaOrKotlin()
        ) {
            return
        }

        lines.addSingleCommentLine("the time the search gave the service to finish with this message")
        lines.add("Thread.sleep(${config.asyncApiSettleMs})")
        lines.appendSemicolon()
    }

    /**
     * Whether the test has lines that publish the message of [res]: the driver's, or those
     * written from the contract. The same conditions [addActionLinesPerType] writes them on.
     */
    private fun publishes(res: AsyncApiCallResult): Boolean =
        res.getTestScript().any { it.isNotBlank() }
                || KafkaTestClientEmitter.isEmitted(res)

    /**
     * Whether the message of [res] went out, whatever came back, and the test publishes it too.
     */
    private fun isPublished(res: AsyncApiCallResult): Boolean =
        res.getOutcome().let { it != null && it != AsyncApiOutcome.PUBLISH_FAILED } && publishes(res)
    /** Maximum time to await emitted messages or completion of the message handler. */
    private fun completionTimeoutMs(): Long =
        maxOf(config.asyncApiReplyTimeoutMs, config.asyncApiSettleMs).toLong()

    /**
     * Assertions on what [KafkaTestClientEmitter.emitAwait] collected, from what the search saw the service emit.
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
    internal fun writeEmittedAssertions(
        lines: Lines,
        result: AsyncApiCallResult,
        emitted: String,
        payloadPrefix: String,
        bodyPrefix: String
    ) {
        require(format.isJavaOrKotlin()) { "Unsupported format: $format" }
        if (!config.enableBasicAssertions) return
        KafkaTestClientEmitter.observedTopicsOf(result).entries.forEachIndexed { i, (topic, messages) ->

            val onTopic = when {
                format.isJava() -> "$emitted.get(${TestWriterUtils.quotedEscaped(topic, format)})"
                format.isKotlin() -> "$emitted.getValue(${TestWriterUtils.quotedEscaped(topic, format)})"
                else -> throw IllegalArgumentException("No Kafka client is written for $format")
            }

            val size = if (format.isJava()) "$onTopic.size()" else "$onTopic.size"
            lines.addStatement("assertTrue($size >= ${messages.size})")

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
            writeReplyAssertions(assertions, payload, variable, bodyPrefix + i)

            //no block that would only hold a comment, or nothing at all
            val comment = "//"
            val asserts = assertions.toString().lines().any { it.isNotBlank() && !it.trimStart().startsWith(comment) }
            if (!asserts) {
                lines.add(assertions)
                return@forEachIndexed
            }

            lines.addSingleCommentLine("only when it arrived alone, as any other could be the one at the front")
            when {
                format.isJava() -> lines.add("if ($onTopic.size() == 1) {")
                format.isKotlin() -> lines.add("if ($onTopic.size == 1) {")
            }
            lines.indented {
                when {
                    format.isJava() -> lines.add("String $variable = $onTopic.get(0);")
                    format.isKotlin() -> lines.add("val $variable = $onTopic[0]")
                }
                lines.add(assertions)
            }
            lines.add("}")
        }
    }

}
