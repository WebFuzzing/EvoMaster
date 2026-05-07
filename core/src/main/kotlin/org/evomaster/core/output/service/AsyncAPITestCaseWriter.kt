package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.asyncapi.data.AsyncAPIAction
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import java.nio.file.Path

/**
 * Test-case writer for AsyncAPI black-box runs.
 *
 * The starter slice emits a structural JUnit 5 skeleton: one test method per
 * individual, with one comment block per action describing what was published
 * or awaited.  Real Kafka producer/consumer code in the generated test is M6
 * work — keeping the writer this thin lets the search complete and produce
 * a TestSuite without depending on broker libraries at test-runtime.
 */
class AsyncAPITestCaseWriter : ApiTestCaseWriter() {

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        lines.addSingleCommentLine("AsyncAPI test case: ${test.name}")
    }

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testCaseName: String,
        testSuitePath: Path?
    ) {
        val actions = ind.individual.seeMainExecutableActions()
        if (actions.isEmpty()) {
            lines.addSingleCommentLine("(no AsyncAPI actions in this individual)")
            return
        }
        for ((i, action) in actions.withIndex()) {
            val result = ind.seeResult(action.getLocalId())
            addActionLinesPerType(action, i, testCaseName, lines, result!!, testSuitePath, baseUrlOfSut)
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
        if (action !is AsyncAPIAction) return

        lines.addSingleCommentLine(
            "Action #$index: ${action.kind} on '${action.channelAddress}' (operation '${action.operationName}')"
        )

        result.getResultValue("delivery")?.let {
            lines.addSingleCommentLine("delivery: $it")
        }
        result.getResultValue("reply")?.let {
            lines.addSingleCommentLine("reply:    $it")
        }
        result.getResultValue("schemaValid")?.let {
            lines.addSingleCommentLine("schema-valid: $it")
        }
        result.getResultValue("variant")?.let {
            lines.addSingleCommentLine("matched reply variant: $it")
        }

        // TODO M6: emit a real KafkaProducer.send(...) call followed by
        // consumer.poll(...) + assertions.  Parking the runtime side until
        // the test-output milestone so the starter slice can ship without
        // pulling kafka-clients into the generated tests' classpath.
        lines.addSingleCommentLine("TODO emit Kafka producer/consumer call (M6)")
        lines.addEmpty(1)
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean = false
}
