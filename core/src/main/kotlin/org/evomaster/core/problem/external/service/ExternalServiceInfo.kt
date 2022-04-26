package org.evomaster.core.problem.external.service

class ExternalServiceInfo(
        remoteHostname: String,
        val protocol: String,
        val remotePort: Int) {

    init {
        if (remoteHostname.isBlank()) {
            throw IllegalArgumentException("Remote hostname can not be blank")
        }
        if (protocol.isBlank()) {
            throw IllegalArgumentException("Protocol can not be blank")
        }
    }
}