package org.evomaster.core.output.naming

import org.evomaster.core.TestUtils
import org.evomaster.core.TestUtils.generateFakeDbAction
import org.evomaster.core.mongo.MongoDbAction
import org.evomaster.core.mongo.MongoDbActionResult
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceAction
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceInfo
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.HttpWsExternalService
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.tracer.Traceable
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionResult
import java.util.*
import java.util.Collections.singletonList
import javax.ws.rs.core.MediaType

object RestActionTestCaseUtils {

    fun getEvaluatedIndividualWith(restAction: RestCallAction): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, 200, "", MediaType.TEXT_PLAIN_TYPE)
    }

    fun getEvaluatedIndividualWith(restAction: RestCallAction, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, 200, "", MediaType.TEXT_PLAIN_TYPE, withSql, withMongo, withWireMock)
    }

    fun getEvaluatedIndividualWith(restAction: RestCallAction, statusCode: Int, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWith(restAction, statusCode, "", MediaType.TEXT_PLAIN_TYPE, withSql, withMongo, withWireMock)
    }

    fun getEvaluatedIndividualWith(restAction: RestCallAction, statusCode: Int, resultBodyString: String, resultBodyType: MediaType, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWithFaults(restAction, emptyList(), statusCode, resultBodyString, resultBodyType, withSql, withMongo, withWireMock)
    }

    fun getEvaluatedIndividualWithFaults(restAction: RestCallAction, faults: List<DetectedFault>, statusCode: Int, resultBodyString: String = "", resultBodyType: MediaType = MediaType.TEXT_PLAIN_TYPE, withSql: Boolean = false, withMongo: Boolean = false, withWireMock: Boolean = false): EvaluatedIndividual<RestIndividual> {
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

    fun getRestCallAction(path: String = "/items", verb: HttpVerb = HttpVerb.GET, parameters: MutableList<Param> = mutableListOf()): RestCallAction {
        return RestCallAction("1", verb, RestPath(path), parameters)
    }

    fun getPathParam(paramName: String): Param {
        return PathParam(paramName, CustomMutationRateGene(paramName, StringGene(paramName), 1.0))
    }

    fun getStringQueryParam(paramName: String, wrapped: Boolean = true): Param {
        return getQueryParam(paramName, StringGene(paramName), wrapped)
    }

    fun getBooleanQueryParam(paramName: String): Param {
        return getQueryParam(paramName, BooleanGene(paramName))
    }

    fun getIntegerQueryParam(paramName: String, wrapped: Boolean = true): Param {
        return getQueryParam(paramName, IntegerGene(paramName), wrapped)
    }

    /*
        Since the randomization used to construct the evaluated individuals might set a random boolean value,
        we do this to ensure the one we want for unit testing
     */
    fun ensureGeneValue(evaluatedIndividual: EvaluatedIndividual<RestIndividual>, paramName: String, paramValue: String) {
        val restCallAction = evaluatedIndividual.evaluatedMainActions().last().action as RestCallAction
        (restCallAction.parameters.filter { it.name == paramName }).forEach {
            (it as QueryParam).getGeneForQuery().setValueBasedOn(paramValue)
        }
    }

    private fun getQueryParam(paramName: String, gene: Gene, wrapped: Boolean = true): Param {
        return QueryParam(paramName, if (wrapped) getWrappedGene(paramName, gene) else gene)
    }

    private fun getWrappedGene(paramName: String, gene: Gene): OptionalGene {
        return OptionalGene(paramName, gene)
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
