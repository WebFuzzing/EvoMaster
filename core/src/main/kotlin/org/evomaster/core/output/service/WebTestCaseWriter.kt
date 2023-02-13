package org.evomaster.core.output.service

import org.evomaster.core.output.Lines
import org.evomaster.core.problem.webfrontend.UserActionType
import org.evomaster.core.problem.webfrontend.WebAction
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import java.lang.IllegalStateException

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
        insertionVars: MutableList<Pair<String, String>>
    ) {
        lines.addStatement("$driver.get($baseUrlOfSut)", format)
        addWaitPageToLoad(lines, 5)
        lines.addEmpty()

        if(ind.individual is WebIndividual){
            ind.evaluatedMainActions().forEach {
                addActionLines(it.action, lines, it.result,  baseUrlOfSut)
            }
        }
    }

    private fun addWaitPageToLoad(lines: Lines, seconds : Int = 2){
        lines.addStatement("waitForPageToLoad($driver, $seconds)", format)
        //TODO need to handle init of JS scripts, not just load of page
    }

    override fun addActionLines(action: Action, lines: Lines, result: ActionResult, baseUrlOfSut: String) {

        //TODO add possible wait on CSS selector. if not, stop test???

        val a = action as WebAction
        a.userInteractions.forEach {
            when(it.userActionType){
                UserActionType.CLICK -> lines.addStatement("clickAndWaitPageLoad($driver, \"${it.cssSelector}\")", format)
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