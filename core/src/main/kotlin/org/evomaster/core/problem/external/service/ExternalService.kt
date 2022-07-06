package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer

class ExternalService (
    val externalServiceInfo: ExternalServiceInfo,
    private val wireMockServer: WireMockServer
        ) {

    /**
     * Return the IP address of WireMock instance
     */
    fun getWireMockAddress() : String {
        return wireMockServer.options.bindAddress()
    }

    fun getWireMockServer() : WireMockServer {
        return wireMockServer
    }

    /**
     * To get all the HTTP/S requests made to the WireMock instance
     *
     * TODO: Filter only the events which doesn't have no match
     */
    fun getRequests() : MutableList<ExternalServiceRequest> {
        return wireMockServer.allServeEvents.map {
            ExternalServiceRequest(
                it.id,
                it.request.absoluteUrl
            )
        }.toMutableList()
    }

    /**
     * Reset WireMock to clean up the requests
     */
    fun reset() {
        wireMockServer.resetAll()
    }

    /**
     * To stop the WireMock server
     */
    fun stopWireMockServer() {
        wireMockServer.stop()
    }

}