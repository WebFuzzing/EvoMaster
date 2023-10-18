package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class HostnameAction(
    private val remoteHostname: String,
    val resolved: Boolean
) : EnvironmentAction(listOf()) {

    fun getHostname(): String {
        return remoteHostname
    }

    override fun getName(): String {
        return "Hostname_${remoteHostname}_${resolved}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return HostnameAction(remoteHostname, resolved)
    }

}
