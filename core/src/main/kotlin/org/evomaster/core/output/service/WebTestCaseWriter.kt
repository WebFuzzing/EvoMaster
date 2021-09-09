package org.evomaster.core.output.service

import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionResult
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.SqlWriter
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.search.EvaluatedDbAction
import org.evomaster.core.search.EvaluatedIndividual

abstract class WebTestCaseWriter : TestCaseWriter() {

    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>) {

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
                    lines, skipFailure = config.skipFailureSQLInTestFile)
        }
    }
}