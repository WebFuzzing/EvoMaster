package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.output.naming.rest.RestActionTestCaseNamingStrategy
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class EnvironmentActionsInTestCaseNamesTest {

    companion object {
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
        const val NO_QUERY_PARAMS_IN_NAME = false
        const val MAX_NAME_LENGTH = 80
    }

    @Test
    fun testIfLessThanTenIndividualsSomeWithSqlSuffixIsPresent() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..4) {
            inds.add(getEvaluatedIndividualWith(restAction, true))
            inds.add(getEvaluatedIndividualWith(restAction, false))
        }
        inds.add(getEvaluatedIndividualWith(restAction, true))
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(9, testCases.size)
        assertEquals(5, testCases.count { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testIfLessThanTenIndividualsAllWithSqlNoneUseSuffix() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..9) {
            inds.add(getEvaluatedIndividualWith(restAction, true))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(9, testCases.size)
        assertTrue(testCases.stream().noneMatch { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testIf10IndividualsWithSqlNoneUseSuffix() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..10) {
            inds.add(getEvaluatedIndividualWith(restAction, true))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(10, testCases.size)
        assertTrue(testCases.stream().noneMatch { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testWhenAtLeast10IndividualsIfHalfHaveSqlItIsNotIncludedInName() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..5) {
            inds.add(getEvaluatedIndividualWith(restAction, true))
            inds.add(getEvaluatedIndividualWith(restAction, false))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(10, testCases.size)
        assertTrue(testCases.stream().noneMatch { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testWhenAtLeast10IndividualsIfLessThanHalfHaveSqlItIsIncludedInName() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..4) {
            inds.add(getEvaluatedIndividualWith(restAction, true))
        }
        for (i in 1..6) {
            inds.add(getEvaluatedIndividualWith(restAction, false))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(10, testCases.size)
        assertEquals(4, testCases.count { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testIfMoreThan10IndividualsWithMongoNoneUseSuffix() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..6) {
            inds.add(getEvaluatedIndividualWith(restAction, withMongo = true))
            inds.add(getEvaluatedIndividualWith(restAction, withMongo = false))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(12, testCases.size)
        assertTrue(testCases.stream().noneMatch { it.name.endsWith("UsingSql") })
    }

    @Test
    fun testIfMoreThan10IndividualsWithWireMockNoneUseSuffix() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..6) {
            inds.add(getEvaluatedIndividualWith(restAction, withWireMock = true))
            inds.add(getEvaluatedIndividualWith(restAction, withWireMock = false))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(12, testCases.size)
        assertTrue(testCases.stream().noneMatch { it.name.endsWith("UsingSql") })
    }

}
