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

    fun stopWireMockServer() {
        wireMockServer.stop()
    }
}