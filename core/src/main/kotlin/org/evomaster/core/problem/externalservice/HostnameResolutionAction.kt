package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class HostnameResolutionAction(
    val hostname: String,
    val localIPAddress: String
) : EnvironmentAction(listOf()) {

    /**
     * Available will check for active Mock Server for the remote hostname
     */
    fun isAvailable(): Boolean {
        val ind = getRoot()

        if (ind !is Individual) {
            throw IllegalStateException("The action is not part of an individual")
        }

        if (ind.searchGlobalState == null) {
            throw IllegalStateException("Search Global State was not setup for the individual")
        }

        return ind.searchGlobalState!!.externalServiceHandler.hasActiveMockServer(hostname)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostnameResolutionAction

        if (hostname != other.hostname) return false
        if (localIPAddress != other.localIPAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hostname.hashCode()
        result = 31 * result + localIPAddress.hashCode()
        return result
    }


    override fun getName(): String {
        return "Hostname_${hostname}_${localIPAddress}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return HostnameResolutionAction(hostname, localIPAddress)
    }
}
