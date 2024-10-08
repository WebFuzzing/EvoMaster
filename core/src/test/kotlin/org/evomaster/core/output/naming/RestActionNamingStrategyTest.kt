package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.EMConfig
import org.evomaster.core.TestUtils
import org.evomaster.core.TestUtils.generateFakeDbAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.mongo.MongoDbActionResult
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.HostnameResolutionAction
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList


class RestActionNamingStrategyTest {

    @Test
    fun testGetOnRootPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_root_returns_200", testCases[0].name)
    }

    @Test
    fun testGetOnItemsPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/items")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_200", testCases[0].name)
    }

    @Test
    fun testTwoDifferentIndividualsGetDifferentNames() {
        val rootIndividual = getEvaluatedIndividualWith("/")
        val itemsIndividual = getEvaluatedIndividualWith("/items")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(mutableListOf(rootIndividual, itemsIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_GET_on_root_returns_200", testCases[0].name)
        assertEquals("test_1_GET_on_items_returns_200", testCases[1].name)
    }

    @Test
    fun testGetOnItemReturnsSingularPathInName() {
        val eIndividual = getEvaluatedIndividualWith("/items/{itemId}")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_item_returns_200", testCases[0].name)
    }

    @Test
    fun test500ResponseNamedWithInternalServerError() {
        val eIndividual = getEvaluatedIndividualWithFaults("/items", 500, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")))
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_causes500_internalServerError", testCases[0].name)
    }

    @Test
    fun testResponseNamedWithMultipleFaults() {
        val faults = listOf(DetectedFault(FaultCategory.GQL_ERROR_FIELD, "items"), DetectedFault(FaultCategory.HTTP_INVALID_LOCATION, "items"), DetectedFault(FaultCategory.HTTP_STATUS_500, "items"))
        val eIndividual = getEvaluatedIndividualWithFaults("/items", 500, faults)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_showsFaults_100_102_301", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSql() {
        val eIndividual = getEvaluatedIndividualWith("/items", 200, true)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturns200UsingSql", testCases[0].name)
    }

    //    @Test// TODO check PhG Gene#isLocallyValid with JP/AA
    fun testIndividualUsingMongo() {
        val eIndividual = getEvaluatedIndividualWith("/items", 200, false, true)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, EMConfig())

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturns200UsingMongo", testCases[0].name)
    }

    @Test
    fun testIndividualUsingWireMock() {
        val eIndividual = getEvaluatedIndividualWith("/items", 200, false, false, true)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val config = EMConfig()
        config.externalServiceIPSelectionStrategy = EMConfig.ExternalServiceIPSelectionStrategy.DEFAULT
        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, config)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturns200UsingWiremock", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSqlWireMock() {
        val eIndividual = getEvaluatedIndividualWith("/items", 200, true, false, true)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val config = EMConfig()
        config.externalServiceIPSelectionStrategy = EMConfig.ExternalServiceIPSelectionStrategy.DEFAULT
        val namingStrategy = RestActionTestCaseNamingStrategy(solution, languageConventionFormatter, config)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturns200UsingSqlWiremock", testCases[0].name)
    }

    private fun getEvaluatedIndividualWith(path: String, statusCode: Int = 200, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWithFaults(path, statusCode, emptyList(), withSql, withMongo, withWireMock)
    }

    private fun getEvaluatedIndividualWithFaults(path: String, statusCode: Int, faults: List<DetectedFault>, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        val restAction = getRestCallAction(path)
        val sqlAction = getSqlAction()
        val mongoDbAction = getMongoDbAction()
        val wireMockAction = getWireMockAction()

        val restResourceCall = RestResourceCalls(actions= listOf(restAction), sqlActions = listOf())

        val actions = mutableListOf<ActionComponent>()
        var sqlSize = 0
        if (withSql) {
            actions.add(sqlAction)
            sqlSize++
        }

        var mongoSize = 0
        if (withMongo) {
            actions.add(mongoDbAction)
            mongoSize++
        }

        var wireMockSize = 0
        if (withWireMock) {
            actions.add(wireMockAction)
            wireMockSize++
        }

        actions.add(restResourceCall)

        val individual = RestIndividual(
            SampleType.RANDOM,
            null,
            null,
            Traceable.DEFAULT_INDEX,
            actions,
            1,
            sqlSize,
            mongoSize,
            wireMockSize
        )

        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val restResult = RestCallResult(restAction.getLocalId())
        restResult.setStatusCode(statusCode)
        faults.forEach { fault -> restResult.addFault(fault) }

        val results = mutableListOf<ActionResult>(restResult)
        if (withSql) results.add(SqlActionResult(sqlAction.getLocalId()))
        if (withMongo) results.add(MongoDbActionResult(mongoDbAction.getLocalId()))

        return EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
    }

    private fun getRestCallAction(path: String): RestCallAction {
        return RestCallAction("1", HttpVerb.GET, RestPath(path), mutableListOf())
    }

    private fun getSqlAction(): SqlAction {
        return generateFakeDbAction(12345L, 1001L, "Foo", 0)
    }

    private fun getMongoDbAction(): MongoDbAction {
        return MongoDbAction(
            "someDatabase",
            "someCollection",
            "\"CustomType\":{\"CustomType\":{\"type\":\"object\", \"properties\": {\"aField\":{\"type\":\"integer\"}}, \"required\": []}}"
        )
    }

    private fun getWireMockAction(): HostnameResolutionAction {
        return HostnameResolutionAction("localhost", "127.0.0.1")
    }
}
