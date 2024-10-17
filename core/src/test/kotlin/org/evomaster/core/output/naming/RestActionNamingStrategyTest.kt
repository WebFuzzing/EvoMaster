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
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceInfo
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.Solution
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import java.util.Collections.singletonList
import javax.ws.rs.core.MediaType


class RestActionNamingStrategyTest {

    companion object {
        val pythonFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
    }

    @Test
    fun testGetOnRootPathIsIncludedInName() {
        val restAction = getRestCallAction("/")
        val eIndividual = getEvaluatedIndividualWith(restAction)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_root_returns_empty", testCases[0].name)
    }

    @Test
    fun testGetOnItemsPathIsIncludedInName() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_empty", testCases[0].name)
    }

    @Test
    fun testTwoDifferentIndividualsGetDifferentNames() {
        val rootAction = getRestCallAction("/")
        val rootIndividual = getEvaluatedIndividualWith(rootAction)

        val itemsAction = getRestCallAction()
        val itemsIndividual = getEvaluatedIndividualWith(itemsAction)
        val solution = Solution(mutableListOf(rootIndividual, itemsIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_GET_on_root_returns_empty", testCases[0].name)
        assertEquals("test_1_GET_on_items_returns_empty", testCases[1].name)
    }

    @Test
    fun testGetOnItemReturnsSingularPathInName() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_item_returns_empty", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsXmlContent() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "<tag/>", MediaType.TEXT_XML_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_content", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsEmptyList() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_empty_list", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsListWithOneElement() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[1]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_1_element", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsListWithThreeElements() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "[1,2,3]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_3_elements", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsEmptyObject() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "{}", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_empty_object", testCases[0].name)
    }

    @Test
    fun testGetOnItemsReturnsObject() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "{\"key\": \"value\"}", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_object", testCases[0].name)
    }

    @Test
    fun testGetOnItemReturnsString() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction, 200, "\"myItem\"", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_item_returns_string", testCases[0].name)
    }

    @Test
    fun testUnauthorizedGetUsesStatusCode() {
        val restAction = getRestCallAction("/items/{itemId}")
        val eIndividual = getEvaluatedIndividualWith(restAction, 401, "[]", MediaType.APPLICATION_JSON_TYPE)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_item_returns_401", testCases[0].name)
    }

    @Test
    fun test500ResponseNamedWithInternalServerError() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWithFaults(restAction, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")), 500)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_causes500_internalServerError", testCases[0].name)
    }

    @Test
    fun testResponseNamedWithMultipleFaults() {
        val faults = listOf(DetectedFault(FaultCategory.GQL_ERROR_FIELD, "items"), DetectedFault(FaultCategory.HTTP_INVALID_LOCATION, "items"), DetectedFault(FaultCategory.HTTP_STATUS_500, "items"))
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWithFaults(restAction, faults, 500)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, pythonFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_showsFaults_100_102_301", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSql() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturnsEmptyUsingSql", testCases[0].name)
    }

    @Test
    fun testIndividualUsingMongo() {
        val restAction = getRestCallAction()
        val eIndividual = getEvaluatedIndividualWith(restAction, withMongo = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_getOnItemsReturnsEmptyUsingMongo", testCases[0].name)
    }

    @Test
    fun testIndividualUsingWireMock() {
        val restAction = getRestCallAction("/items", HttpVerb.PUT)
        val eIndividual = getEvaluatedIndividualWith(restAction, 201, withWireMock = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_putOnItemsReturns201UsingWiremock", testCases[0].name)
    }

    @Test
    fun testIndividualUsingSqlWireMock() {
        val restAction = getRestCallAction("/items", HttpVerb.POST)
        val eIndividual = getEvaluatedIndividualWith(restAction, withSql = true, withWireMock = true)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_postOnItemsReturns200UsingSqlWiremock", testCases[0].name)
    }

    private fun getEvaluatedIndividualWith(restAction: RestCallAction): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, 200, "", MediaType.TEXT_PLAIN_TYPE)
    }

    private fun getEvaluatedIndividualWith(restAction: RestCallAction, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, 200, "", MediaType.TEXT_PLAIN_TYPE, withSql, withMongo, withWireMock)
    }

    private fun getEvaluatedIndividualWith(restAction: RestCallAction, statusCode: Int, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, statusCode, "", MediaType.TEXT_PLAIN_TYPE, withSql, withMongo, withWireMock)
    }

    private fun getEvaluatedIndividualWith(restAction: RestCallAction, statusCode: Int, resultBodyString: String, resultBodyType: MediaType, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWithFaults(restAction, emptyList(), statusCode, resultBodyString, resultBodyType, withSql, withMongo, withWireMock)
    }

    private fun getEvaluatedIndividualWithFaults(restAction: RestCallAction, faults: List<DetectedFault>, statusCode: Int, resultBodyString: String = "", resultBodyType: MediaType = MediaType.TEXT_PLAIN_TYPE, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
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
            0
        )

        TestUtils.doInitializeIndividualForTesting(individual, Randomness())


        val restResult = getRestCallResult(restAction.getLocalId(), statusCode, resultBodyString, resultBodyType)
        faults.forEach { fault -> restResult.addFault(fault) }

        val results = mutableListOf<ActionResult>(restResult)
        if (withSql) results.add(SqlActionResult(sqlAction.getLocalId()))
        if (withMongo) results.add(MongoDbActionResult(mongoDbAction.getLocalId()))
        if (withWireMock) {
            val parentAction = individual.seeMainExecutableActions()[0].parent
            if (parentAction != null) {
                wireMockAction.doInitialize(Randomness())
                parentAction.addChildrenToGroup(singletonList(wireMockAction), GroupsOfChildren.EXTERNAL_SERVICES)
                results.add(getRestCallResult(parentAction.getLocalId(), statusCode, resultBodyString, resultBodyType))
            }
        }

        return EvaluatedIndividual<RestIndividual>(FitnessValue(0.0), individual, results)
    }

    private fun getRestCallAction(path: String = "/items", verb: HttpVerb = HttpVerb.GET): RestCallAction {
        return RestCallAction("1", verb, RestPath(path), mutableListOf())
    }

    private fun getRestCallResult(sourceLocalId: String, statusCode: Int, resultBodyString: String, bodyType: MediaType): RestCallResult {
        val restResult = RestCallResult(sourceLocalId)
        restResult.setStatusCode(statusCode)
        restResult.setBody(resultBodyString)
        restResult.setBodyType(bodyType)
        return restResult
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

    private fun getWireMockAction(): HttpExternalServiceAction {
        val request = HttpExternalServiceRequest(
            UUID.randomUUID(),"GET","http://noname.local:12354/api/mock","http://noname.local:12354/api/mock",true,
            UUID.randomUUID().toString(),"http://noname.local:12354/api/mock", mapOf(),null)
        val serviceInfo = HttpExternalServiceInfo("HTTP", "noname.local", 12354)
        val service = HttpWsExternalService(serviceInfo, "localhost")
        return HttpExternalServiceAction(request, "", service, 1L)
    }
}
