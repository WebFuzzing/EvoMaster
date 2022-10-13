package org.evomaster.core.problem.external.service.httpws

import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils.getDefaultWMHttpPort
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils.getDefaultWMHttpsPort

class DefaultHttpExternalServiceInfo private constructor(protocol: String, remotePort: Int)
    : HttpExternalServiceInfo(protocol, "no_host_name", remotePort) {

    companion object{
        fun createDefaultHttps() = DefaultHttpExternalServiceInfo("https", getDefaultWMHttpsPort())
        fun createDefaultHttp() = DefaultHttpExternalServiceInfo("http", getDefaultWMHttpPort())
    }

    override fun signature(): String {
        return ExternalServiceSharedUtils.getWMDefaultSignature(protocol,remotePort)
    }
}