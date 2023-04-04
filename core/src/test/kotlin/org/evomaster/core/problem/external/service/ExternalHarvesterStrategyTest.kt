package org.evomaster.core.problem.external.service

import com.alibaba.dcm.DnsCacheManipulator
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.netflix.governator.guice.LifecycleInjector
import org.evomaster.core.BaseModule
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.externalservice.httpws.HttpExternalServiceRequest
import org.evomaster.core.problem.externalservice.httpws.param.HttpWsResponseParam
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


class ExternalHarvesterStrategyTest {

    private lateinit var config: EMConfig
    private lateinit var externalHarvestActualHttpWsResponseHandler: HarvestActualHttpWsResponseHandler

    @BeforeEach
    fun init(){
        val injector: Injector = LifecycleInjector.builder()
            .withModules(FakeModule(), BaseModule())
            .build().createInjector()


        config = injector.getInstance(EMConfig::class.java)
        externalHarvestActualHttpWsResponseHandler = injector.getInstance(HarvestActualHttpWsResponseHandler::class.java)


    }

    private class FakeModule : AbstractModule() {

        @Provides
        @Singleton
        fun getRemoteController(): RemoteController {
            return DummyController()
        }

        override fun configure() {
            bind(HarvestActualHttpWsResponseHandler::class.java)
                .asEagerSingleton()
        }
    }

    @Test
    fun testExactStrategy() {
        val wm = WireMockServer(WireMockConfiguration()
            .bindAddress("127.0.0.2")
            .port(12354)
            .extensions(ResponseTemplateTransformer(false)))
        wm.start()
        wm.stubFor(
            WireMock.get(
                WireMock.urlEqualTo("/api/mock"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(200).withBody("{\"message\" : \"Working\"}"))
        )

        DnsCacheManipulator.setDnsCache("noname.local", "127.0.0.2")


        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.EXACT
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

        externalHarvestActualHttpWsResponseHandler.initialize()

        val resultRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://noname.local:12354/api/mock","http://noname.local:12354/api/mock",true,UUID.randomUUID().toString(),"http://noname.local:12354/api/mock", mapOf(),null)
        val noResponseRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://noname.local:12354/api/mock/neverthere","http://noname.local:12354/api/mock/neverthere",true,UUID.randomUUID().toString(),"http://noname.local:12354/api/mock/neverthere", mapOf(),null)

        val requests = mutableListOf<HttpExternalServiceRequest>()
        requests.add(resultRequest)

        externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)


        Thread.sleep(3000)

        val successResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(resultRequest, 1.0) as HttpWsResponseParam
        val noResponseResult = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(noResponseRequest, 1.0)

        val successStatus = successResult!!.status.values[successResult!!.status.index]

        wm.shutdown()
        DnsCacheManipulator.clearDnsCache()

        assertEquals(successStatus, 200)
        assertEquals(noResponseResult, null)

    }

    @Test
    fun testClosestStrategy() {
        val wm = WireMockServer(WireMockConfiguration()
            .bindAddress("127.0.0.3")
            .port(12354)
            .extensions(ResponseTemplateTransformer(false)))
        wm.start()
        wm.stubFor(
            WireMock.get(
                WireMock.urlEqualTo("/api/mock"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(200).withBody("{\"message\" : \"Working\"}"))
        )

        DnsCacheManipulator.setDnsCache("exists.local", "127.0.0.3")

        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.CLOSEST
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

        externalHarvestActualHttpWsResponseHandler.initialize()

        val resultRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://exists.local:12354/api/mock","http://exists.local:12354/api/mock",true,UUID.randomUUID().toString(),"http://exists.local:12354/api/mock", mapOf(),null)
        val secondRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://exists.local:12354/api/mock","http://exists.local:12354/api/mock",true,UUID.randomUUID().toString(),"http://exists.local:12354/api/mock", mapOf(),null)

        val requests = mutableListOf<HttpExternalServiceRequest>()
        requests.add(resultRequest)

        externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)


        val noResponseRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://neverthere.local/api","http://neverthere.local/api",true,UUID.randomUUID().toString(),"http://neverthere.local/api", mapOf(),null)

        Thread.sleep(3000)

        val successResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(resultRequest, 1.0) as HttpWsResponseParam
        val possibleForResultExistence: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(secondRequest, 1.0) as HttpWsResponseParam
        val noResponseResult = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(noResponseRequest, 1.0)

        val successStatus = successResult!!.status.values[successResult!!.status.index]

        wm.shutdown()
        DnsCacheManipulator.clearDnsCache()

        assertEquals(successStatus, 200)
        assertEquals(possibleForResultExistence, 200)
        assertEquals(noResponseResult, null)
    }

    @Test
    fun testRandomStrategy() {
        val wm = WireMockServer(WireMockConfiguration()
            .bindAddress("127.0.0.4")
            .port(12354)
            .extensions(ResponseTemplateTransformer(false)))
        wm.start()
        wm.stubFor(
            WireMock.get(
                WireMock.urlEqualTo("/api/mock"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(200).withBody("{\"message\" : \"Working\"}"))
        )

        DnsCacheManipulator.setDnsCache("exists.local", "127.0.0.4")

        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.RANDOM
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

        externalHarvestActualHttpWsResponseHandler.initialize()

        val resultRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://exists.local:12354/api/mock","http://exists.local:12354/api/mock",true,UUID.randomUUID().toString(),"http://exists.local:12354/api/mock", mapOf(),null)
        val secondDomainRequest = HttpExternalServiceRequest(UUID.randomUUID(),"GET","http://neverthere.local/api","http://neverthere.local/api",true,UUID.randomUUID().toString(),"http://neverthere.local/api", mapOf(),null)

        val requests = mutableListOf<HttpExternalServiceRequest>()
        requests.add(resultRequest)

        externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)

        Thread.sleep(3000)

        val successResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(resultRequest, 1.0) as HttpWsResponseParam
        val secondDomainResult: HttpWsResponseParam = externalHarvestActualHttpWsResponseHandler.getACopyOfActualResponse(secondDomainRequest, 1.0) as HttpWsResponseParam

        val successStatus = successResult!!.status.values[successResult!!.status.index]
        val secondDomainStatus = successResult!!.status.values[secondDomainResult!!.status.index]

        wm.shutdown()
        DnsCacheManipulator.clearDnsCache()

        assertEquals(successStatus, 200)
        assertEquals(secondDomainStatus, 200)
    }
}