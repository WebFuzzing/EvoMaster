package org.evomaster.core.problem.externalservice

import org.evomaster.core.search.EnvironmentAction
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class DnsAction(
    private val hostname: String,
    val resolved: Boolean,
) : EnvironmentAction(listOf()) {

    fun getHostname(): String {
        return hostname
    }
    override fun getName(): String {
        return "Hostname_${hostname}_${resolved}"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf()
    }

    override fun copyContent(): StructuralElement {
        return DnsAction(hostname, resolved)
    }
}
