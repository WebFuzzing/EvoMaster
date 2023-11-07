package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class HostnameResolutionAction(
    val hostname: String,
    val resolvedAddress: String,
    val resolved: Boolean
) : EnvironmentAction(listOf()) {

    fun getRemoteHostname(): String { return hostname }

    /**
     * Available will check for ip!=null && ip!=default.
     * Default here is a fallback service running locally to prevent SUT from connecting
     * to the real one.
     */
    fun isAvailable(): Boolean { return resolvedAddress != "" }

    override fun getName(): String {
        return "Hostname_${hostname}_${resolvedAddress}_${resolved}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return HostnameResolutionAction(hostname, resolvedAddress, resolved)
    }
}
