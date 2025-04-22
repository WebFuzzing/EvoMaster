package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import java.nio.file.Path

class NoTestCaseWriter : TestCaseWriter() {
    override fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testName: String
    ) {
        // empty
    }

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>, testCaseName: String, testSuitePath: Path?) {
        //empty
    }

    override fun addActionLinesPerType(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {
        //empty
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
        return false
    }

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        //TODO
    }
}
