package org.evomaster.core.output.service

import com.google.inject.Inject
import org.evomaster.core.output.Lines
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCCallResult
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.service.RPCEndpointsHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.gene.GeneUtils
import kotlin.math.max

/**
 * created by manzhang on 2021/11/26
 */
class RPCTestCaseWriter : WebTestCaseWriter() {

    @Inject
    protected lateinit var rpcHandler: RPCEndpointsHandler

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>
    ) {

        if (ind.individual is RPCIndividual){
            ind.evaluatedActions().forEach { evaluatedAction->

                lines.addEmpty()

                val call = evaluatedAction.action as RPCCallAction
                val res = evaluatedAction.result as RPCCallResult

                if (res.failedCall()) {
                    addActionInTryCatch(call, lines, res, baseUrlOfSut)
                } else {
                    addActionLines(call, lines, res, baseUrlOfSut)
                }
            }
        }
    }

    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String) {

        val rpcCallAction = (action as? RPCCallAction)?: throw IllegalStateException("action must be RPCCallAction, but it is ${action::class.java.simpleName}")
        val rpcCallResult = (result as? RPCCallResult)?: throw IllegalStateException("result must be RPCCallResult, but it is ${action::class.java.simpleName}")

        val executionJson = rpcHandler.getRPCActionJson(rpcCallAction)

        val resVarName = createUniqueResponseVariableName()

        lines.addEmpty()

        /*
            TODO we might generate RPC action with fully instanced values
            eg, instance all required dto with code

            currently, perform RPC endpoint execution with Sut Handler using string json which indicates detailed action info
         */
        executeActionWithSutHandler(lines, resVarName, executionJson)

        appendAdditionalInfo(lines, rpcCallResult)

    }

    private fun appendAdditionalInfo(lines: Lines, result: RPCCallResult){
        // here, we report internal error and unexpected exception as potential faults
        if (config.outputFormat.isJavaOrKotlin() && result.hasPotentialFault()){
            lines.append("// ${result.getLastStatementForPotentialBug()}")
            if (result.isExceptionThrown())
                lines.append(" ${result.getExceptionInfo()}")
        }

    }

    override fun shouldFailIfException(result: ActionResult): Boolean {
        //TODO Man: need a further check
        return false
    }

    private fun executeActionWithSutHandler(lines: Lines, resVarName: String, executionJson: String){
        when {
            format.isKotlin() -> lines.add("val $resVarName = ${TestSuiteWriter.controller}.executeRPCEndpoint(")
            format.isJava() -> lines.add("Object $resVarName = ${TestSuiteWriter.controller}.executeRPCEndpoint(")
        }

        printExecutionJson(executionJson, lines)

        when {
            format.isKotlin() -> lines.append(")")
            format.isJava() -> lines.append(");")
        }
    }

    private fun printExecutionJson(json: String, lines: Lines) {

        val body = if (OutputFormatter.JSON_FORMATTER.isValid(json)) {
            OutputFormatter.JSON_FORMATTER.getFormatted(json)
        } else {
            json
        }

        val bodyLines = body.split("\n").map { s ->
            // after applyEscapes, somehow, the format is changed, then count space here
            countFirstSpace(s) to "\" " + GeneUtils.applyEscapes(s.trim(), mode = GeneUtils.EscapeMode.BODY, format = format) + " \""
        }

        printBodyLines(lines, bodyLines)
    }

    private fun printBodyLines(lines: Lines, bodyLines: List<Pair<Int, String>>){
        lines.indented {
            bodyLines.forEachIndexed { index, line->
                lines.add(nSpace(line.first)+ line.second + (if (index != bodyLines.size - 1) "+" else ""))
            }
        }
    }

    private fun countFirstSpace(line: String) : Int{
        return max(0, line.indexOfFirst { it != ' ' })
    }

    private fun nSpace(n: Int): String{
        return (0 until n).joinToString("") { " " }
    }

}