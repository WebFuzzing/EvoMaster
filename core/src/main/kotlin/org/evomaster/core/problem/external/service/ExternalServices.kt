package org.evomaster.core.problem.external.service

class ExternalServices {
    /**
     * This will hold the information about the external service
     * calls inside the SUT. Information will be passed to the core
     * through AdditionalInfoDto and will be captured under
     * AbstractRestFitness and AbstractRestSample for further use.
     */

    /**
     * Set contains the information about each external calls made
     */
    val externalServiceInfos: MutableSet<ExternalServiceInfo> = mutableSetOf()

    /**
     * This will allow adding ExternalServiceInfo to the set
     */
    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        externalServiceInfos.add(externalServiceInfo)
    }

    /**
     * Implemented for testing purposes, ignore this.
     * Will be refactored once the test is completed
     */
    fun getExternalServicesCount() : Int {
        return externalServiceInfos.size
    }
}