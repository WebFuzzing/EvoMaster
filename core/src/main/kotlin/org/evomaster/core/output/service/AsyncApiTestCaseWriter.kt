package org.evomaster.core.output.service

import org.evomaster.core.output.asyncapi.AsyncApiReplyAssertions
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
class AsyncApiTestCaseWriter : ApiTestCaseWriter() {

    /**
     * The Kafka servers this suite publishes to.
     * Key: the server's name, ie its key under `servers` in the AsyncAPI document.
     * Value: the server's `host` as declared in the document, eg "localhost:9092", used when
     * the driver does not give an address.
     *
     * Filled in when the class members are written, and read when the init statements are,
     * which the suite writer does afterwards.
     */
    private val servers = LinkedHashMap<String, String>()

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
        val call = action as? AsyncApiAction
            ?: throw IllegalArgumentException("Not an AsyncAPI action: ${action::class.java.simpleName}")
        val res = result as? AsyncApiCallResult
            ?: throw IllegalArgumentException("Not an AsyncAPI result: ${result::class.java.simpleName}")

        lines.addSingleCommentLine("${call.operationId}: ${res.getOutcome() ?: "not executed"}")

        val script = res.getTestScript()
        val variable = res.getReplyVariableName()

        when {
            //rendered by the driver, for transports not written from the contract
            script.any { it.isNotBlank() } -> script.forEach { lines.add(it) }

            variable != null && KafkaTestClientEmitter.canEmit(res) ->
                KafkaTestClientEmitter.emit(lines, res, variable, brokerOf(res), format)

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

        addReplyAssertions(lines, res)
    }

    /**
     * What the core can say about the reply, on top of whatever the driver's own lines assert.
     */
    private fun addReplyAssertions(lines: Lines, res: AsyncApiCallResult) {

        if (res.getOutcome() != AsyncApiOutcome.REPLIED) {
            return
        }

        val variable = res.getReplyVariableName() ?: return

        lines.add("assertNotNull($variable)")
        lines.appendSemicolon()

        res.getReplyMessage()?.let {
            lines.addSingleCommentLine("recognised as the declared message '$it'")
        }

        AsyncApiReplyAssertions(config, format, ::isFieldToSkip, ::isSuitableToPrint)
            .emit(lines, res.getReplyPayload(), variable, bodyVariableFor(variable))
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
        servers.clear()

        if (kafkaResults.isEmpty()) {
            return
        }

        if (!canAskDriver()) {
            return
        }

        kafkaResults.forEach { r ->
            val name = r.getServerName()
            if (name.isNullOrBlank()) {
                return@forEach
            }
            //names like "a-b" and "a.b" map to the same variable: only the first gets one
            if (servers.keys.any { it != name && variableFor(it) == variableFor(name) }) {
                return@forEach
            }
            servers.putIfAbsent(name, r.getBroker() ?: "")
        }

        servers.keys.forEach { name ->
            when {
                format.isJava() -> lines.add("private static String ${variableFor(name)};")
                format.isKotlin() -> lines.add("private lateinit var ${variableFor(name)}: String")
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
        }
        lines.addEmpty()
    }

    /**
     * Ask the driver where each server is, falling back to the address in the document.
     */
    override fun addExtraInitStatement(lines: Lines) {

        servers.forEach { (name, declared) ->
            val variable = variableFor(name)
            val ask = "${TestSuiteWriter.controller}.getAsyncApiServerAddress(" +
                    TestWriterUtils.quotedEscaped(name, format) + ")"
            val fallback = TestWriterUtils.quotedEscaped(declared, format)

            when {
                //two statements, so the driver is asked only once
                format.isJava() -> {
                    lines.add("$variable = $ask;")
                    lines.add("if ($variable == null) {")
                    lines.indented {
                        lines.add("$variable = $fallback;")
                    }
                    lines.add("}")
                }
                format.isKotlin() -> lines.add("$variable = $ask ?: $fallback")
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
        }
    }

    /**
     * Where to publish: the variable filled in from the driver, or the document's address when
     * there is no driver.
     */
    private fun brokerOf(result: AsyncApiCallResult): String {

        val name = result.getServerName()

        if (canAskDriver() && name != null && servers.containsKey(name)) {
            return variableFor(name)
        }

        return TestWriterUtils.quotedEscaped(result.getBroker() ?: "", format)
    }

    /**
     * Name of the variable holding the parsed reply.
     */
    private fun bodyVariableFor(replyVariable: String) =
        bodyVariablePrefix + replyVariable.substringAfterLast('_')

    private fun variableFor(serverName: String) =
        KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX + TestWriterUtils.safeVariableName(serverName)

    /**
     * Whether the generated suite has a driver to ask where a server is.
     */
    private fun canAskDriver() = (!config.blackBox || config.bbExperiments) && format.isJavaOrKotlin()

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
