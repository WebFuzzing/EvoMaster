package org.evomaster.core.problem.external.service

import com.github.tomakehurst.wiremock.WireMockServer

class ExternalServiceInfo(
        val protocol: String,
        val remoteHostname: String,
        val remotePort: Int) {

    private lateinit var wireMockServer : WireMockServer

    init {
        if (remoteHostname.isBlank()) {
            throw IllegalArgumentException("Remote hostname can not be blank")
        }
        if (protocol.isBlank()) {
            throw IllegalArgumentException("Protocol can not be blank")
        }
    }

    fun assignWireMockServer(wm: WireMockServer) {
        wireMockServer = wm
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExternalServiceInfo

        if (protocol != other.protocol) return false
        if (remoteHostname != other.remoteHostname) return false
        if (remotePort != other.remotePort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = protocol.hashCode()
        result = 31 * result + remoteHostname.hashCode()
        result = 31 * result + remotePort
        return result
    }
}