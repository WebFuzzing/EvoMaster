package org.evomaster.core.problem.externalservice.httpws

import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils.getDefaultWMHttpPort
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils.getDefaultWMHttpsPort

/**
 * as the default behavior, instead of connecting to the real external services,
 * we make the SUT connect to our specified default WM as this info
 */
class DefaultHttpExternalServiceInfo private constructor(protocol: String, remotePort: Int)
    : HttpExternalServiceInfo(protocol, ExternalServiceSharedUtils.DEFAULT_WM_DUMMY_HOSTNAME, remotePort) {

    companion object{
        fun createDefaultHttps() = DefaultHttpExternalServiceInfo("https", getDefaultWMHttpsPort())
        fun createDefaultHttp() = DefaultHttpExternalServiceInfo("http", getDefaultWMHttpPort())
    }

    override fun signature(): String {
        return ExternalServiceSharedUtils.getWMDefaultSignature(protocol,remotePort)
    }
}
