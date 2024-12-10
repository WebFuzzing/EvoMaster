package org.evomaster.core.problem.externalservice.httpws

import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.core.problem.externalservice.ExternalService


open class HttpExternalServiceInfo(
    val protocol: String,
    val remoteHostname: String,
    var remotePort: Int
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
    open fun signature(): String {
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

    /**
     * @return whether the protocol is clear https
     */
    fun isHttps() = protocol.equals("https", ignoreCase = true)

    /**
     * @return whether the protocol is clear http
     */
    fun isHttp() = protocol.equals("http", ignoreCase = true)

    /**
     * @return whether the protocol is likely https based on the given port when protocol is neither https nor http
     */
    fun isDerivedHttps() : Boolean = ExternalServiceSharedUtils.isHttps(protocol, remotePort)

    fun getDescriptiveURLPath() : String = "${if (isHttps() || (!isHttp() && isDerivedHttps())) "https" else "http"}://${remoteHostname}:${remotePort}"

    /**
     * To check whether the ExternalServiceInfo has all the information available or not
     */
    fun isPartial() : Boolean = remotePort < 0

    fun updateRemotePort(port: Int) {
        remotePort = port
    }

    fun toExternalService() : ExternalService {
        return ExternalService(remoteHostname, remotePort)
    }
}
