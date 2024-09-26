package org.evomaster.core.output.naming

import org.evomaster.core.search.Solution

class TestCaseNamingStrategyFactory(
    private val namingStrategy: NamingStrategy
) {

    fun create(solution: Solution<*>): TestCaseNamingStrategy {
        if (namingStrategy.isNumbered()) {
            return NumberedTestCaseNamingStrategy(solution)
        }
        throw IllegalStateException("Unrecognized naming strategy " + namingStrategy)
    }
}
