package org.evomaster.core.problem.external.service

class ExternalServices {

    val externalServiceInfos: MutableSet<ExternalServiceInfo> = mutableSetOf()

    fun addExternalService(externalServiceInfo: ExternalServiceInfo) {
        externalServiceInfos.add(externalServiceInfo)
    }
}