package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer

class ExternalService (
    val externalServiceInfo: ExternalServiceInfo,
    private val wireMockServer: WireMockServer
        ) {

    fun stopWireMockServer() {
        wireMockServer.stop()
    }
}