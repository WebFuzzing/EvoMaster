package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ParamMapping (
    val name: String,
    val description: String?
) {

    /**
     * Holds potential vulnerability class for the [Param].
     * Key contains the vulnerability class, and value marks the exploitability
     * using a [Boolean].
     */
    var vulnerabilityClasses: MutableMap<VulnerabilityClass, Boolean> = mutableMapOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses[vulnerabilityClass] = false
    }

    fun hasVulnerabilityClass(vulnerabilityClass: VulnerabilityClass): Boolean {
        return vulnerabilityClasses.contains(vulnerabilityClass)
    }

}
