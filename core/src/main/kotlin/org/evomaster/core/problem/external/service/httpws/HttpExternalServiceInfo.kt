package org.evomaster.core.problem.external.service.httpws

import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import java.math.BigInteger
import java.security.MessageDigest


class HttpExternalServiceInfo(
    val protocol: String,
    val remoteHostname: String,
    val remotePort: Int
) {

    init {
        if (remoteHostname.isBlank()) {
            throw IllegalArgumentException("Remote hostname can not be blank")
        }
        if (protocol.isBlank()) {
            throw IllegalArgumentException("Protocol can not be blank")
        }
    }

    /**
     * Generates an identifier based on the [protocol], [remoteHostname], and [remotePort]
     * then returns it as String.
     * Will be used in WireMock as its identifier to simplify the tracking.
     */
    fun signature(): String {
        return ExternalServiceSharedUtils.getSignature(protocol,remoteHostname,remotePort)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HttpExternalServiceInfo

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