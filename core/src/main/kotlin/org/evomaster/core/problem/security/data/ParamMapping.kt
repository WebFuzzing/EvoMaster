package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ParamMapping (
    val name: String,
    val description: String?,
) {

    var promptId : String? = null

    var paramType: ParameterType? = null

    var vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses.add(vulnerabilityClass)
    }

}
