package org.evomaster.core.problem.external.service

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

    /**
     * This will allow adding ExternalServiceInfo to the set
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        externalServiceInfos.add(externalServiceInfo)
    }

    fun getExternalServices() : MutableList<ExternalServiceInfo> {
        return externalServiceInfos
    }
}