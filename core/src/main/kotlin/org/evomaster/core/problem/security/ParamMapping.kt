package org.evomaster.core.problem.security

class ParamMapping (
    val name: String,
    val description: String,
) {

    val vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        this.vulnerabilityClasses.add(vulnerabilityClass)
    }
}
