package org.evomaster.core.output.service

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.output.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedDbAction
import org.evomaster.core.search.EvaluatedIndividual

abstract class WebTestCaseWriter : TestCaseWriter() {

    protected fun createUniqueResponseVariableName(): String {
        val name = "res_$counter"
        counter++
        return name
    }

    protected fun createUniqueBodyVariableName(): String {
        val name = "body_$counter"
        counter++
        return name
    }

    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>, insertionVars: MutableList<Pair<String, String>>) {

        CookieWriter.handleGettingCookies(format, ind, lines, baseUrlOfSut, this)
        TokenWriter.handleGettingTokens(format,ind, lines, baseUrlOfSut, this)

        val initializingActions = ind.individual.seeInitializingActions().filterIsInstance<DbAction>()
        val initializingActionResults = (ind.seeResults(initializingActions))
        if(initializingActionResults.any { (it as? DbActionResult)  == null})
            throw IllegalStateException("the type of results are expected as DbActionResults")


        if (ind.individual.seeInitializingActions().isNotEmpty()) {
            SqlWriter.handleDbInitialization(
                    format,
                    initializingActions.indices.map {
                        EvaluatedDbAction(initializingActions[it], initializingActionResults[it] as DbActionResult) },
                    lines, insertionVars = insertionVars, skipFailure = config.skipFailureSQLInTestFile)
        }
    }
}