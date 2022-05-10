package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.evomaster.core.problem.external.service.ExternalServiceUtils.isAddressAvailable

class ExternalServiceAction {

    fun initWireMockForService(address: String) {
        if (!isAddressAvailable(address, WIREMOCK_PORT)) {
            throw Exception("Address unavailable")
        }
        val wireMockServer = WireMockServer(
            WireMockConfiguration()
                .bindAddress(address)
                .port(WIREMOCK_PORT)
                .extensions(ResponseTemplateTransformer(false)))
        wireMockServer.start()
    }

    companion object {
        private const val WIREMOCK_PORT : Int = 8080
    }

}