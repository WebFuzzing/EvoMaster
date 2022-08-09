package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.stubbing.StubMapping

/**
 * Represent the external service related information including
 * WireMock server and ExternalServiceInfo collected from SUT.
 */
class ExternalService(
    /**
     * External service information collected from SUT
     */
    val externalServiceInfo: ExternalServiceInfo,
    /**
     * Initiated WireMock server for the external service
     */
    private val wireMockServer: WireMockServer
) {

    /**
     * Return the IP address of WireMock instance
     */
    fun getWireMockAddress(): String {
        return wireMockServer.options.bindAddress()
    }

    /**
     * Return the running port of WireMock instance
     */
    fun getWireMockPort(): Int {
        return wireMockServer.options.portNumber()
    }

    fun getWireMockServer(): WireMockServer {
        return wireMockServer
    }

    fun getWireMockAbsoluteAddress(): String {
        return getWireMockAddress().plus(":").plus(getWireMockPort())
    }

    /**
     * Returns the active stub mappings from the WireMockServer
     */
    fun getStubs(): List<StubMapping> {
        return wireMockServer.stubMappings
    }


    /**
     * To get all the HTTP/S requests made to the WireMock instance
     *
     * TODO: For now watMatched serves no purpose. Should be handled when
     *  handling diff for the received requests
     *
     * TODO: Query parameters are not available under ServeEvent for some
     *  reasons. Need to check why.
     */
    fun getAllServedRequests(): List<ExternalServiceRequest> {
        return wireMockServer.allServeEvents.map {
            ExternalServiceRequest(
                it.id,
                it.request.method.value(),
                it.request.url,
                it.request.absoluteUrl,
                it.wasMatched,
            )
        }.toList()
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

    fun getSignature(): String {
        return externalServiceInfo.remoteHostname
            .replace(".", "")
            .lowercase()
            .plus(externalServiceInfo.remotePort)
    }

}