package org.evomaster.core.output.service

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
 * Unlike REST, there is no universal client to call: a message goes out over whichever broker
 * the contract names, so the lines that publish it and read the reply are the driver's, rendered
 * while the search ran and pasted here. This follows RPC, whose driver renders an invocation the
 * same way, and it is what keeps a generated test standing on its own: it talks to the broker
 * with an ordinary client of that transport, and needs no EvoMaster driver at run time.
 *
 * What the core adds around those lines is what only it knows: which outcome the action had,
 * which of the declared reply messages the reply was recognised as, and that a reply promised by
 * the contract did arrive.
 */
class AsyncApiTestCaseWriter : ApiTestCaseWriter() {

    override fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        sqlInsertionVars: MutableList<Pair<String, String>>,
        mongoInsertionVars: MutableList<Pair<String, String>>,
        redisInsertionVars: MutableList<Pair<String, String>>,
        dynamoDbInsertionVars: MutableList<Pair<String, String>>,
        testName: String
    ) {
        super.handleTestInitialization(
            lines,
            baseUrlOfSut,
            ind,
            sqlInsertionVars,
            mongoInsertionVars,
            redisInsertionVars,
            dynamoDbInsertionVars,
            testName
        )
    }

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        sqlInsertionVars: MutableList<Pair<String, String>>,
        mongoInsertionVars: MutableList<Pair<String, String>>,
        redisInsertionVars: MutableList<Pair<String, String>>,
        dynamoDbInsertionVars: MutableList<Pair<String, String>>,
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
            script.isNotEmpty() -> script.forEach { lines.add(it) }

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

        lines.add("assertNotNull($variable)")
        lines.appendSemicolon()

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
     * The servers this suite publishes to, by the name the document gives them, each with the
     * address the document declares. Filled in while the class members are written, and read
     * again when the init statements are, which run after them.
     */
    private val servers = LinkedHashMap<String, String>()

    /**
     * The helper the Kafka tests call, written once and only when something in the suite calls
     * it: a suite that publishes over another transport must not be made to carry a Kafka
     * dependency it never uses.
     *
     * The address of each server is written here too, when the suite has a driver to ask. The
     * document's address is where the author deployed it, which is not where a system started
     * for testing is: a broker in a container answers on a port chosen at start-up. This is the
     * same reason the base URL of a REST system is taken from the driver rather than the
     * document.
     */
    override fun addExtraClassMembers(lines: Lines, solution: Solution<*>) {

        val kafkaResults = solution.individuals
            .flatMap { it.evaluatedMainActions() }
            .mapNotNull { it.result as? AsyncApiCallResult }
            .filter { it.getTestScript().isEmpty() && KafkaTestClientEmitter.canEmit(it) }

        if (kafkaResults.isEmpty()) {
            return
        }

        KafkaTestClientEmitter.emitHelper(lines, format)

        if (!hasDriver()) {
            return
        }

        servers.clear()
        kafkaResults.forEach { r ->
            val name = r.getServerName() ?: return@forEach
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
            val ask = "${TestSuiteWriter.controller}.getAsyncApiServerAddress(\"$name\")"
            val fallback = KafkaTestClientEmitter.brokerLiteral(declared, format)

            when {
                format.isJava() -> lines.add("$variable = $ask != null ? $ask : $fallback;")
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

        if (hasDriver() && name != null && servers.containsKey(name)) {
            return variableFor(name)
        }

        return KafkaTestClientEmitter.brokerLiteral(result.getBroker() ?: "", format)
    }

    /**
     * What the parsed reply is called, beside the reply it was parsed from.
     */
    private fun bodyVariableFor(replyVariable: String) =
        "body_" + replyVariable.substringAfterLast('_')

    private fun variableFor(serverName: String) =
        KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX + TestWriterUtils.safeVariableName(serverName)

    /**
     * Whether the generated suite has a driver in it at all. A black-box suite does not, so
     * there is nothing to ask and the document's address is all there is.
     */
    private fun hasDriver() = !config.blackBox || config.bbExperiments

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
