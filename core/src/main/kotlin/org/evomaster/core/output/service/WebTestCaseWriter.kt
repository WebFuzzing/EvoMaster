package org.evomaster.core.output.service

import org.evomaster.core.database.DbAction
import org.evomaster.core.output.CookieWriter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.SqlWriter
import org.evomaster.core.output.TokenWriter
import org.evomaster.core.search.EvaluatedIndividual

abstract class WebTestCaseWriter : TestCaseWriter() {

    override fun handleFieldDeclarations(lines: Lines, baseUrlOfSut: String, ind: EvaluatedIndividual<*>) {

        CookieWriter.handleGettingCookies(format, ind, lines, baseUrlOfSut)
        TokenWriter.handleGettingTokens(format,ind, lines, baseUrlOfSut)

        if (ind.individual.seeInitializingActions().isNotEmpty()) {
            SqlWriter.handleDbInitialization(
                    format,
                    ind.individual.seeInitializingActions().filterIsInstance<DbAction>(),
                    lines, skipFailure = config.skipFailureSQLInTestFile)
        }
    }
}