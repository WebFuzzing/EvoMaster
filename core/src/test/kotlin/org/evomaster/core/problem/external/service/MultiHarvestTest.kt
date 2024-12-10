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
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.remote.service.RemoteController
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Duration
import java.util.*

class MultiHarvestTest {

    private lateinit var config: EMConfig
    private lateinit var externalHarvestActualHttpWsResponseHandler: HarvestActualHttpWsResponseHandler

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
    fun testMultiThreadHarvestClient(){
        val count = 3

        val injector: Injector = LifecycleInjector.builder()
                .withModules(FakeModule(), BaseModule(arrayOf("--probOfHarvestingResponsesFromActualExternalServices","1.0", "--externalRequestHarvesterNumberOfThreads", "$count")))
                .build().createInjector()


        config = injector.getInstance(EMConfig::class.java)
        externalHarvestActualHttpWsResponseHandler = injector.getInstance(HarvestActualHttpWsResponseHandler::class.java)


        val host = "clientthreadTest.local"
        val ip = "127.0.0.6"
        val port = 12345
        val pathPrefix = "/api/thread"
        val queryParam = "foo"

        val wm = WireMockServer(WireMockConfiguration()
                .bindAddress(ip)
                .port(port)
                .extensions(ResponseTemplateTransformer(false)))
        wm.start()

        val num = 3
        val delay = 500 // millisecond

        (0 until num).forEach {
            wm.stubFor(
                    WireMock.get(
                            WireMock.urlMatching("$pathPrefix$it\\?$queryParam=\\d+"))
                            .atPriority(1)
                            .willReturn(
                                    WireMock.aResponse()
                                            .withStatus(200)
                                            .withBody("{\"message\" : \"$it\"}")
                                            .withFixedDelay(delay)
                            )
            )
        }

        DnsCacheManipulator.setDnsCache(host, ip)


        val amount = 100
        val requests = (0 until amount).flatMap {
            (0 until num).map{r->
                val url = "http://$host:$port$pathPrefix$r"
                val fullURL = "$url?$queryParam=$it"
                HttpExternalServiceRequest(
                        UUID.randomUUID(),"GET",fullURL,fullURL,true, UUID.randomUUID().toString(),fullURL, mapOf(),null)
            }
        }

        /*
            in total 3 * 100 = 300 requests
            0.5s per request

            if we send x requests in parallel, it will take roughly 150/x seconds.
            then it should finish within 150/x + 4 seconds as Timeout

            note that GA might only have 2 cores, then count specified as 3 might not happen in GA
         */

        val total = amount * num * delay / 1000
        val timeout = (total / externalHarvestActualHttpWsResponseHandler.getConfiguredFixedThreadPool()) + 4L

        assertTimeout(Duration.ofSeconds(timeout), "Fail to harvest $total requests within $timeout using ${externalHarvestActualHttpWsResponseHandler.getConfiguredFixedThreadPool()} threads"){
            externalHarvestActualHttpWsResponseHandler.addHttpRequests(requests)

            while (externalHarvestActualHttpWsResponseHandler.getNumOfHarvestedResponse() < amount * num){
                Thread.sleep(100)
            }

            wm.shutdown()
            DnsCacheManipulator.clearDnsCache()

            assertEquals(externalHarvestActualHttpWsResponseHandler.getConfiguredFixedThreadPool(), externalHarvestActualHttpWsResponseHandler.getNumOfClients())
            externalHarvestActualHttpWsResponseHandler.shutdown()
        }

    }
}