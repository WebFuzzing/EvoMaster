package org.evomaster.core.problem.external.service

class ExternalServices {

    val externalServices: MutableSet<ExternalService> = mutableSetOf()

    fun addExternalService(externalService: ExternalService) {
        externalServices.add(externalService)
    }
}