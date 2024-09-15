package org.evomaster.core.output.service.naming

import org.evomaster.core.search.Solution

class TestCaseNamingStrategyFactory(
    private val namingStrategy: NamingStrategy
) {

    fun create(solution: Solution<*>): TestCaseNamingStrategy {
        if (namingStrategy.isNumbered()) {
            return NumberedTestCaseNamingStrategy(solution)
        }
        return NumberedTestCaseNamingStrategy(solution)
    }
}
