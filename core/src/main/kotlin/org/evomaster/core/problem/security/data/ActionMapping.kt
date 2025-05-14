package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ActionMapping (
    val name: String,

) {

    var paramMapping: Map<String, ParamMapping> = mutableMapOf()

    var isVulnerable = false

    var isExploitable = false

    var vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses.add(vulnerabilityClass)
    }
}
