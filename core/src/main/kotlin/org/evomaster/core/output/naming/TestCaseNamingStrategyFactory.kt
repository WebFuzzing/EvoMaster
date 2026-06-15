package org.evomaster.core.output.naming

import org.evomaster.core.EMConfig
import org.evomaster.core.output.naming.rest.RestActionTestCaseNamingStrategy
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.search.Solution
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCaseNamingStrategyFactory(
    private val namingStrategy: NamingStrategy,
    private val languageConventionFormatter: LanguageConventionFormatter,
    private val nameWithQueryParameters: Boolean,
    private val maxTestCaseNameLength: Int
) {

    constructor(config: EMConfig): this(config.namingStrategy, LanguageConventionFormatter(config.outputFormat), config.nameWithQueryParameters, config.maxTestCaseNameLength)

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestCaseNamingStrategyFactory::class.java)
    }

    fun create(solution: Solution<*>): TestCaseNamingStrategy {
        return when(namingStrategy) {
            NamingStrategy.NUMBERED -> NamingHelperNumberedTestCaseNamingStrategy(solution)
            NamingStrategy.DETERMINISTIC -> deterministicActionBasedNamingStrategy(solution)
            //TODO LLM
            else -> throw IllegalStateException("Unrecognized naming strategy $namingStrategy")
        }
    }

    private fun deterministicActionBasedNamingStrategy(solution: Solution<*>): NumberedTestCaseNamingStrategy {
        val individuals = solution.individuals
        return when {
            individuals.any { it.individual is RestIndividual } ->  RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, nameWithQueryParameters, maxTestCaseNameLength)
            individuals.any { it.individual is GraphQLIndividual } ->  GraphQLActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength)
            individuals.any { it.individual is RPCIndividual } ->  RPCActionTestCaseNamingStrategy(solution, languageConventionFormatter, maxTestCaseNameLength)
            individuals.any { it.individual is WebIndividual } -> {
                log.warn("Web individuals do not have action based test case naming yet. Defaulting to Numbered strategy.")
                NamingHelperNumberedTestCaseNamingStrategy(solution)
            }
            individuals.isEmpty() -> {
                log.warn("No individuals present in the solution. Defaulting to Numbered strategy.")
                NumberedTestCaseNamingStrategy(solution)
            }
            else -> throw IllegalStateException("Unrecognized test individuals with no action based naming strategy set.")
        }
    }

}
