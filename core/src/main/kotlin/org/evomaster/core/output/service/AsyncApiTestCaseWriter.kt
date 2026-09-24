package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.search.EvaluatedIndividual
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

        if (script.isEmpty()) {
            /*
                Nothing can be written that would reach the service: publishing needs a client of
                the transport, and only the driver has one. Said plainly rather than left to a
                reader wondering why a test does nothing.
             */
            lines.addSingleCommentLine(
                "The driver rendered no lines for this message, so there is nothing to publish here."
            )
            lines.addSingleCommentLine(
                "See SutController.executeAsyncApiAction for what a driver fills in."
            )
            return
        }

        script.forEach { lines.add(it) }

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
    }

    /**
     * A message that could not be published stops the test, and the search already recorded why,
     * so there is no exception for a generated test to expect.
     */
    override fun shouldFailIfExceptionNotThrown(result: ActionResult) = false

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        //the per-action comments carry the outcome, which is what there is to say
    }
}
