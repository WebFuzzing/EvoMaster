package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.stubbing.ServeEvent

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

    /**
     * To get all the HTTP/S requests made to the WireMock instance
     */
    fun getRequests() : MutableList<ServeEvent>? {
        return wireMockServer.allServeEvents
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