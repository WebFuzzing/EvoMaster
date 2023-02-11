package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual

class NoTestCaseWriter : TestCaseWriter() {
    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>) {
        //empty
    }

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>) {
        //empty
    }

    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String) {
        //empty
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
        return false
    }
}