package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class HostnameResolutionAction(
    val hostname: String,
    val resolvedAddress: String,
    val resolved: Boolean,
) : EnvironmentAction(listOf()) {

    fun getRemoteHostname(): String { return hostname }

    override fun getName(): String {
        return "Hostname_${hostname}_${resolvedAddress}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return HostnameResolutionAction(hostname, resolvedAddress, resolved)
    }
}
