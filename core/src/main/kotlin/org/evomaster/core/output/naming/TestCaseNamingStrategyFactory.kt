package org.evomaster.core.output.naming

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCaseNamingStrategyFactory(
    private val namingStrategy: NamingStrategy,
    private val languageConventionFormatter: LanguageConventionFormatter
) {

    constructor(config: EMConfig): this(config.namingStrategy, LanguageConventionFormatter(config.outputFormat))

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestCaseNamingStrategyFactory::class.java)
    }

    fun create(solution: Solution<*>): TestCaseNamingStrategy {
        if (namingStrategy.isNumbered()) {
            return NamingHelperNumberedTestCaseNamingStrategy(solution)
        } else if (namingStrategy.isAction()) {
            if (solution.individuals.any { it.individual is RestIndividual }) {
                return ActionTestCaseNamingStrategy(solution, languageConventionFormatter)
            } else {
                log.warn("Action based naming strategy only available for REST APIs at the moment. Defaulting to Numbered strategy.")
                return NamingHelperNumberedTestCaseNamingStrategy(solution)
            }
        }
        throw IllegalStateException("Unrecognized naming strategy $namingStrategy")
    }
}
