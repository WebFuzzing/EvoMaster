package org.evomaster.core.output.service.naming

import org.evomaster.core.output.TestCase

interface TestCaseNamingStrategy {

    fun getTestCases(): List<TestCase>

}
