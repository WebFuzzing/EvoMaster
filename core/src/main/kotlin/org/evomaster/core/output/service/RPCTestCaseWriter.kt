package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.problem.httpws.service.HttpWsAction
import org.evomaster.core.problem.httpws.service.HttpWsCallResult
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual

/**
 * created by manzhang on 2021/11/26
 */
class RPCTestCaseWriter : HttpWsTestCaseWriter() {
    override fun getAcceptHeader(call: HttpWsAction, res: HttpWsCallResult): String {
        TODO("Not yet implemented")
    }

    override fun handleVerbEndpoint(baseUrlOfSut: String, _call: HttpWsAction, lines: Lines) {
        TODO("Not yet implemented")
    }

    override fun handleActionCalls(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>) {
        TODO("Not yet implemented")
    }

    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String) {
        TODO("Not yet implemented")
    }
}