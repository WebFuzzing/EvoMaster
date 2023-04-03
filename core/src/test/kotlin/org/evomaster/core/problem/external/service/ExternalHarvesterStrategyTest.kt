package org.evomaster.core.problem.external.service

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
import org.junit.jupiter.api.Assertions.assertEquals
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
        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.EXACT
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

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
        config.externalRequestResponseSelectionStrategy = EMConfig.ExternalRequestResponseSelectionStrategy.CLOSEST
        config.probOfHarvestingResponsesFromActualExternalServices = 1.0
        config.probOfMutatingResponsesBasedOnActualResponse = 2.0

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