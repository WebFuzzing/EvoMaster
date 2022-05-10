package org.evomaster.core.problem.external.service

import org.evomaster.core.problem.external.service.ExternalServiceUtils.nextIPAddress

class ExternalServices {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     */

    /**
     * Contains the information about each external calls made
     */
    private val externalServiceInfos: MutableList<ExternalServiceInfo> = mutableListOf()

    private val lastIPAddress : String

    /**
     * This will allow adding ExternalServiceInfo to the set
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        externalServiceInfos.add(externalServiceInfo)
    }

    fun addExternalService(externalServiceInfoDtos: List<ExternalServiceInfo>) {
        externalServiceInfoDtos.forEach {
            val externalServiceInfo = ExternalServiceInfo(
                    it.protocol,
                    it.remoteHostname,
                    it.remotePort
            )
//            externalServiceInfo.initService(getNextAvailableAddress())
            externalServiceInfos.add(externalServiceInfo)

            // 127.0.0.2 - 127.0.0.254
            // 127.0.1.1 - 127.0.1.254
            // 127.255.255.254
            // google.com - 127.0.0.2
            // google.com:9000 - 127.0.0.8/2
            // google.com:5000 -
        }
    }

    private fun getNextAvailableAddress(): String {
        val nextAddress = nextIPAddress(lastIPAddress)
        if (nextAddress == "127.255.255.255") {
            throw Exception("Next IP address out of usable range")
        }
        return nextAddress
    }


    fun getExternalServices() : List<ExternalServiceInfo> {
        return externalServiceInfos
    }
}