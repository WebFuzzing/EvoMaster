package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.output.TestCase
import org.evomaster.core.problem.webfrontend.*
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import java.lang.IllegalStateException
import java.nio.file.Path

class WebTestCaseWriter : TestCaseWriter() {

    private val driver : String = TestSuiteWriter.driver

    override fun handleTestInitialization(
        lines: Lines,
        baseUrlOfSut: String,
        ind: EvaluatedIndividual<*>,
        insertionVars: MutableList<Pair<String, String>>,
        testName: String
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
        lines.addStatement("goToPage($driver, $baseUrlOfSut, 5)")

        if(ind.individual is WebIndividual){
            ind.evaluatedMainActions().forEachIndexed { index,  a ->
                addActionLines(a.action, index, testCaseName, lines, a.result, testSuitePath, baseUrlOfSut)
            }
            val lastEvaluated = ind.evaluatedMainActions().last()
            val lastAction = lastEvaluated.action as WebAction
            val lastResult = lastEvaluated.result as WebResult
            val url =  if(!lastResult.stopping){
                 lastResult.getUrlPageEnd()!!
            } else {
                //if stopping, it means nothing could be done, and no info on where it went.
                //it also implies that such entry itself was printed out in the test
                assert(lastAction.userInteractions.isEmpty())
                lastResult.getUrlPageStart()!!
            }
            lines.add(getCommentOnPage("ended on page", url,null,lastResult.getValidHtml()))
        }
    }

    private fun addWaitPageToLoad(lines: Lines, seconds : Int = 2){
        lines.addStatement("waitForPageToLoad($driver, $seconds)")
        //TODO need to handle init of JS scripts, not just load of page
    }

    private fun getCommentOnPage(label: String, start: String, end: String?, validHtml: Boolean?) : String{
        var comment = " // $label ${HtmlUtils.getPathAndQueries(start)}"
        if(validHtml == false){
            comment += "  (ERRORS in HTML in reached page ${HtmlUtils.getPathAndQueries(end!!)})"
        }
        return comment
    }

    override fun addActionLinesPerType(action: Action, index: Int, testCaseName: String, lines: Lines, result: ActionResult, testSuitePath: Path?, baseUrlOfSut: String) {

        //TODO add possible wait on CSS selector. if not, stop test???

        val a = action as WebAction
        val r = result as WebResult
        a.userInteractions.forEach {
            when(it.userActionType){
                UserActionType.CLICK -> {
                    lines.addStatement("clickAndWaitPageLoad($driver, \"${it.cssSelector}\")")
                    lines.append(getCommentOnPage("on page", r.getUrlPageStart()!!, r.getUrlPageEnd(), r.getValidHtml()))
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

    override fun addTestCommentBlock(lines: Lines, test: TestCase) {
        //TODO
    }
}
