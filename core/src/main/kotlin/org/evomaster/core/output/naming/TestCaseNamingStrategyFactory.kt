package org.evomaster.core.output.naming

import org.evomaster.core.search.Solution

class TestCaseNamingStrategyFactory(
    private val namingStrategy: NamingStrategy
) {

    fun create(solution: Solution<*>): TestCaseNamingStrategy {
        if (namingStrategy.isNumbered()) {
            return NamingHelperNumberedTestCaseNamingStrategy(solution)
        } else if (namingStrategy.isAction()) {
            return ActionTestCaseNamingStrategy(solution)
        }
        throw IllegalStateException("Unrecognized naming strategy $namingStrategy")
    }
}
