package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWithFaults
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.output.naming.rest.RestActionTestCaseNamingStrategy
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import java.util.Collections.singletonList
import javax.ws.rs.core.MediaType


open class RestActionNamingStrategyTest {

    companion object {
        val pythonFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
        const val NO_QUERY_PARAMS_IN_NAME = false
        const val MAX_NAME_LENGTH = 80
    }

    @Test
    fun testGetOnRootPathIsIncludedInName() {
        val restAction = getRestCallAction("/")
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_root_returns_empty", testCases[0].name)
    }

    @Test
    fun testGetOnItemsPathIsIncludedInName() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_empty", testCases[0].name)
    }

    @Test
    fun testTwoDifferentIndividualsGetDifferentNames() {
        val rootAction = getRestCallAction("/")
        val rootIndividual = getEvaluatedIndividualWith(rootAction)

        val itemsAction = getRestCallAction()
        val itemsIndividual = getEvaluatedIndividualWith(itemsAction)
        val solution = Solution(mutableListOf(rootIndividual, itemsIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(2, testCases.size)
        assertEquals("test_0_get_on_root_returns_empty", testCases[0].name)
        assertEquals("test_1_get_on_items_returns_empty", testCases[1].name)
    }

    @Test
    fun testGetOnItemReturnsSingularPathInName() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_item_returns_empty", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsXmlContent() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "<tag/>", MediaType.TEXT_XML_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_content", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsEmptyList() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_empty_list", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsListWithOneElement() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[1]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_1_element", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsListWithThreeElements() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[1,2,3]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_3_elements", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsEmptyObject() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "{}", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_empty_object", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsObject() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "{\"key\": \"value\"}", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_returns_object", testCases[0].name)
    }

    @Test
    fun testGetOnItemReturnsString() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "\"myItem\"", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_item_returns_string", testCases[0].name)
    }

    @Test
    fun testUnauthorizedGetUsesStatusCode() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction, 401, "[]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_item_returns_401", testCases[0].name)
    }

    @Test
    fun test500ResponseNamedWithInternalServerError() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWithFaults(restAction, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")), 500)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_causes500_internalServerError", testCases[0].name)
    }

    @Test
    fun testResponseNamedWithMultipleFaults() {
        val faults = listOf(DetectedFault(FaultCategory.GQL_ERROR_FIELD, "items"), DetectedFault(FaultCategory.HTTP_INVALID_LOCATION, "items"), DetectedFault(FaultCategory.HTTP_STATUS_500, "items"))
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWithFaults(restAction, faults, 500)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_get_on_items_showsFaults_100_102_301", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSql() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturnsEmptyUsingSql", testCases[0].name)
    }

    @Test
    fun testIndividualUsingMongo() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, withMongo = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturnsEmptyUsingMongo", testCases[0].name)
    }

    @Test
    fun testIndividualUsingWireMock() {
        val restAction = getRestCallAction("/items", HttpVerb.PUT)
        val eIndividual = getEvaluatedIndividualWith(restAction, 201, withWireMock = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_putOnItemsReturns201UsingWireMock", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSqlWireMock() {
        val restAction = getRestCallAction("/items", HttpVerb.POST)
        val eIndividual = getEvaluatedIndividualWith(restAction, withSql = true, withWireMock = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_postOnItemsReturns200UsingSqlWireMock", testCases[0].name)
    }

    @Test
    fun testNameLengthUsesNumberLengthNotNumberValue() {
        val restAction = getRestCallAction()
        val inds = mutableListOf<EvaluatedIndividual<RestIndividual>>()
        for (i in 1..10) {
            inds.add(getEvaluatedIndividualWith(restAction))
        }
        val solution = Solution(Collections.unmodifiableList(inds), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME, 20)
        val testCases = namingStrategy.getTestCases()

        assertEquals(10, testCases.size)
        assertTrue(testCases.stream().allMatch { it.name.length > "test_10".length })
    }

}
