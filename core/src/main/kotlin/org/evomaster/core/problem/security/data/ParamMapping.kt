package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ParamMapping (
    val name: String,
    val description: String?
) {

    var vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses.add(vulnerabilityClass)
    }

    fun hasVulnerabilityClass(vulnerabilityClass: VulnerabilityClass): Boolean {
        return vulnerabilityClasses.contains(vulnerabilityClass)
    }

}
