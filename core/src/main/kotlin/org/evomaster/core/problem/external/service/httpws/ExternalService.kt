package org.evomaster.core.problem.external.service.httpws

import com.github.tomakehurst.wiremock.WireMockServer
import java.util.*

/**
 * Represent the external service related information including
 * WireMock server and ExternalServiceInfo collected from SUT.
 */
class ExternalService(
    /**
     * External service information collected from SUT
     */
    val externalServiceInfo: HttpExternalServiceInfo,
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

    fun getWireMockServer(): WireMockServer {
        return wireMockServer
    }

    /**
     * To get all the HTTP/S requests made to the WireMock instance
     *
     * TODO: For now watMatched serves no purpose. Should be handled when
     *  handling diff for the received requests
     */
    fun getAllServedRequests(): List<HttpExternalServiceRequest> {
        return wireMockServer.allServeEvents.map {
                HttpExternalServiceRequest(
                    it.id,
                    it.request.method.value(),
                    it.request.url,
                    it.request.absoluteUrl,
                    it.wasMatched,
                )
        }.toList()
    }

    /**
     * Reset WireMock to clean everything including stubs and
     * requests.
     */
    fun reset() {
        wireMockServer.resetAll()
    }

    /**
     * Reset the served request on the respective WireMock
     * instance.
     */
    fun resetServedRequests() {
        wireMockServer.resetRequests()
    }

    /**
     * To stop the WireMock server
     */
    fun stopWireMockServer() {
        wireMockServer.stop()
    }

    /**
     * Will remove the stub mapping related to the given id
     */
    fun removeStub(stubId: UUID) : Boolean {
        val stubMapping = wireMockServer.getStubMapping(stubId)
        if (stubMapping.isPresent) {
            wireMockServer.removeStubMapping(stubMapping.item)
            return true
        }
        return false
    }

}