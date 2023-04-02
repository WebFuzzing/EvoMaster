package org.evomaster.core.problem.external.service

import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ExternalHarvesterStrategyUnitTest {

    val randomness = Randomness()

    val remoteController = DummyController()

    @Test
    fun testExactStrategy() {
        val config = EMConfig()
        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.EXACT
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

        val externalHarvestActualHttpWsResponseHandler = HarvestActualHttpWsResponseHandler(config, remoteController, randomness)
        externalHarvestActualHttpWsResponseHandler.initialize()

        val requests = mutableListOf<HttpExternalServiceRequest>()
        requests.add(HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://www.google.com/","https://www.google.com/",true,UUID.randomUUID().toString(),"https://www.google.com/", mapOf(),null))

        externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)

        val resultRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://www.google.com/","https://www.google.com/",true,UUID.randomUUID().toString(),"https://www.google.com/", mapOf(),null)
        val noResponseRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://www.google.com/maps","https://www.google.com/maps",true,UUID.randomUUID().toString(),"https://www.google.com/maps", mapOf(),null)

        Thread.sleep(3000)

        val successResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(resultRequest, 1.0) as HttpWsResponseParam
        val noResponseResult = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(noResponseRequest, 1.0)

        val successStatus = successResult!!.status.values[successResult!!.status.index]

        assertEquals(successStatus, 200)
        assertEquals(noResponseResult, null)

    }

    @Test
    fun testClosestStrategy() {
        val config = EMConfig()
        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.CLOSEST
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

        val externalHarvestActualHttpWsResponseHandler = HarvestActualHttpWsResponseHandler(config, remoteController, randomness)
        externalHarvestActualHttpWsResponseHandler.initialize()

        val requests = mutableListOf<HttpExternalServiceRequest>()
        requests.add(HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://www.google.com/","https://www.google.com/",true,UUID.randomUUID().toString(),"https://www.google.com/", mapOf(),null))

        externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)

        val resultRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://www.google.com/maps","https://www.google.com/maps",true,UUID.randomUUID().toString(),"https://www.google.com/maps", mapOf(),null)
        val noResponseRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","https://nodomain.com/","https://nodomain.com/",true,UUID.randomUUID().toString(),"https://nodomain.com/", mapOf(),null)

        Thread.sleep(3000)

        val successResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(resultRequest, 1.0) as HttpWsResponseParam
        val noResponseResult = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(noResponseRequest, 1.0)

        val successStatus = successResult!!.status.values[successResult!!.status.index]

        assertEquals(successStatus, 200)
        assertEquals(noResponseResult, null)

    }
}