package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class InputMapping (
    val name: String,
    val description: String?
) {

    /**
     * Holds potential vulnerability class for the [Param].
     * Key contains the vulnerability class, and value marks the exploitability
     * using a [Boolean].
     */
    var vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses.add(vulnerabilityClass)
    }

    fun hasVulnerabilityClass(vulnerabilityClass: VulnerabilityClass): Boolean {
        return vulnerabilityClasses.contains(vulnerabilityClass)
    }

}
