package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.problem.webfrontend.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import java.lang.IllegalStateException
import java.nio.file.Path

class WebTestCaseWriter : TestCaseWriter() {

    private val driver : String = TestSuiteWriter.driver

    override fun handleFieldDeclarations(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>
    ) {
        //nothing to do? at least for now...
    }

    override fun handleActionCalls(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testCaseName: String,
        testSuitePath: Path?
    ) {
        lines.addStatement("goToPage($driver, $baseUrlOfSut, 5)", format)

        if(ind.individual is WebIndividual){
            ind.evaluatedMainActions().forEachIndexed { index,  a ->
                addActionLines(a.action, index, testCaseName, lines, a.result, testSuitePath, baseUrlOfSut)
            }
            val lastResult = ind.evaluatedMainActions().last().result as WebResult
            if(!lastResult.stopping){
                lines.add("//  ${HtmlUtils.getPathAndQueries(lastResult.getUrlPageEnd()!!)}")
            } else {
                lines.add("//  ${HtmlUtils.getPathAndQueries(lastResult.getUrlPageStart()!!)}")
            }
        }
    }

    private fun addWaitPageToLoad(lines: Lines, seconds : Int = 2){
        lines.addStatement("waitForPageToLoad($driver, $seconds)", format)
        //TODO need to handle init of JS scripts, not just load of page
    }

    override fun addActionLines(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {

        //TODO add possible wait on CSS selector. if not, stop test???

        val a = action as WebAction
        val r = result as WebResult
        a.userInteractions.forEach {
            when(it.userActionType){
                UserActionType.CLICK -> {
                    lines.addStatement("clickAndWaitPageLoad($driver, \"${it.cssSelector}\")", format)
                    lines.append(" // ${HtmlUtils.getPathAndQueries(r.getUrlPageStart()!!)}")
                }
                //TODO all other cases
                else -> throw IllegalStateException("Not handled action type: ${it.userActionType}")
            }
        }

        //TODO assertions on action
    }

    override fun shouldFailIfExceptionNotThrown(result: ActionResult): Boolean {
        return false
    }
}