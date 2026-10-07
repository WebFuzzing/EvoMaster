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
 * Unlike REST there is no universal client to call. For Kafka the contract names the server,
 * the topics and the header the correlation id rides in, so the lines that publish and await are
 * written here from it; for a transport it does not describe, the driver renders them while the
 * search runs and they are pasted verbatim, as RPC does. Either way the test needs no EvoMaster
 * driver at run time.
 *
 * What the core adds around them is what only it knows: the outcome, which declared message a
 * reply was recognised as, and that a promised reply arrived.
 */
class AsyncApiTestCaseWriter : ApiTestCaseWriter() {

    /**
     * The servers this suite publishes to, each with the address the document declares. Filled
     * in while the class members are written and read when the init statements are, which the
     * suite writer runs after them.
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
            ?: throw IllegalStateException("Not an AsyncAPI action: ${action::class.java.simpleName}")
        val res = result as? AsyncApiCallResult
            ?: throw IllegalStateException("Not an AsyncAPI result: ${result::class.java.simpleName}")

        lines.addSingleCommentLine("${call.operationId}: ${res.getOutcome() ?: "not executed"}")

        val script = res.getTestScript()
        val variable = res.getReplyVariableName()

        when {
            /*
                The driver rendered the lines itself, which is how a transport the contract does
                not describe well enough gets written: one whose correlation id rides inside the
                service's own message layout, or one nothing here knows at all.
             */
            script.any { it.isNotBlank() } -> script.forEach { lines.add(it) }

            /*
                Kafka is written from the contract, as REST writes its RestAssured calls: the
                document named the broker, the topics and the header the id rides in, so nothing
                else has to.
             */
            variable != null && KafkaTestClientEmitter.canEmit(res) ->
                KafkaTestClientEmitter.emit(lines, res, variable, brokerOf(res), format)

            else -> {
                /*
                    Nothing can be written that would reach the service. Said plainly rather than
                    left to a reader wondering why a test does nothing.
                 */
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

        if (format.isPython()) {
            lines.add("assert $variable is not None")
        } else {
            lines.add("assertNotNull($variable)")
            lines.appendSemicolon()
        }

        res.getReplyMessage()?.let {
            lines.addSingleCommentLine("recognised as the declared message '$it'")
        }

        /*
            What came back, asserted the way REST asserts a response body: from what was actually
            observed, not from what the contract promises. Which of the declared messages a reply
            is turns on the fields it carries, so these assertions are also what keeps a test
            honest about the variant its own name claims.
         */
        AsyncApiReplyAssertions.emit(
            lines,
            res.getReplyPayload(),
            variable,
            bodyVariableFor(variable),
            config,
            format
        )
    }

    /**
     * A message that could not be published stops the test, and the search already recorded why,
     * so there is no exception for a generated test to expect.
     */
    override fun shouldFailIfExceptionNotThrown(result: ActionResult) = false

    /**
     * The helper the Kafka tests call, written once and only when something in the suite calls
     * it: a suite publishing over another transport must not be made to carry a Kafka dependency
     * it never uses.
     *
     * A variable per server goes here too, when there is a driver to ask where it is -- see
     * [org.evomaster.client.java.controller.SutHandler.getAsyncApiServerAddress].
     */
    override fun addExtraClassMembers(lines: Lines, solution: Solution<*>) {

        val kafkaResults = KafkaTestClientEmitter.resultsIn(solution)

        /*
            Before anything may return: one writer writes every suite of a run, so what the last
            one found must not be left behind for the next, which would assign to a field it
            never declared.
         */
        servers.clear()

        if (kafkaResults.isEmpty()) {
            return
        }

        KafkaTestClientEmitter.emitHelper(lines, format)

        if (!canAskDriver()) {
            return
        }

        kafkaResults.forEach { r ->
            val name = r.getServerName()
            if (name.isNullOrBlank()) {
                return@forEach
            }
            /*
                Two names that differ only where a variable cannot, say "a-b" and "a.b", would be
                declared twice under one identifier. The first keeps the variable; the rest fall
                back to the address the document gave them.
             */
            if (servers.keys.any { it != name && variableFor(it) == variableFor(name) }) {
                return@forEach
            }
            servers.putIfAbsent(name, r.getBroker() ?: "")
        }

        servers.keys.forEach { name ->
            when {
                format.isJava() -> lines.add("private static String ${variableFor(name)};")
                format.isKotlin() -> lines.add("private lateinit var ${variableFor(name)}: String")
            }
        }
        lines.addEmpty()
    }

    /**
     * Ask the driver where each server is, falling back to the document when it does not say,
     * which is what a driver that never overrode the method does.
     */
    override fun addExtraInitStatement(lines: Lines) {

        servers.forEach { (name, declared) ->
            val variable = variableFor(name)
            val ask = "${TestSuiteWriter.controller}.getAsyncApiServerAddress(" +
                    KafkaTestClientEmitter.brokerLiteral(name, format) + ")"
            val fallback = KafkaTestClientEmitter.brokerLiteral(declared, format)

            when {
                /*
                    Written over two statements rather than a ternary, so that a driver which
                    computes the address, rather than keeping one, is asked for it only once.
                 */
                format.isJava() -> {
                    lines.add("$variable = $ask;")
                    lines.add("if ($variable == null) {")
                    lines.indented {
                        lines.add("$variable = $fallback;")
                    }
                    lines.add("}")
                }
                format.isKotlin() -> lines.add("$variable = $ask ?: $fallback")
            }
        }
    }

    /**
     * Where a message goes, as the generated test sees it: the variable the suite filled in from
     * the driver, or the document's own address when there is no driver to ask.
     */
    private fun brokerOf(result: AsyncApiCallResult): String {

        val name = result.getServerName()

        if (canAskDriver() && name != null && servers.containsKey(name)) {
            return variableFor(name)
        }

        return KafkaTestClientEmitter.brokerLiteral(result.getBroker() ?: "", format)
    }

    /**
     * What the parsed reply is called, beside the reply it was parsed from.
     */
    private fun bodyVariableFor(replyVariable: String) =
        bodyVariablePrefix + replyVariable.substringAfterLast('_')

    private fun variableFor(serverName: String) =
        KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX + TestWriterUtils.safeVariableName(serverName)

    /**
     * Whether the generated suite can ask a driver where a server is: it needs one in it, which
     * a black-box suite has not, and the controller is only declared for Java and Kotlin.
     */
    private fun canAskDriver() = (!config.blackBox || config.bbExperiments) && format.isJavaOrKotlin()

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {

        val actions = test.test.evaluatedMainActions()

        /*
            What REST puts above a test, with the outcome where it puts the status code: there is
            no code to report, and the outcome is the same kind of one-word summary of what came
            back.
         */
        if (actions.isNotEmpty()) {
            lines.addBlockCommentLine("Calls:")
            actions.forEachIndexed { index, evaluated ->
                val outcome = (evaluated.result as? AsyncApiCallResult)?.getOutcome() ?: "not executed"
                val prefix = if (actions.size == 1) "" else "${index + 1} - "
                lines.addBlockCommentLine("$prefix($outcome) ${evaluated.action.getName()}")
            }
        }

        addFaultsCommentLine(lines, test)
    }
}
