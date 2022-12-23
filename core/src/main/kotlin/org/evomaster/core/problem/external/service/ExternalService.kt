package org.evomaster.core.problem.external.service

class ExternalService (
    private val hostname: String,

    private val port: Int

) {

    fun getHostName(): String {
        return hostname
    }

    fun getPort(): Int {
        return port
    }

}