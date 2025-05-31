package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ActionMapping (
    val name: String,
) {

    var params: Map<String, ParamMapping> = mutableMapOf()

    var isVulnerable = false

    var isExploitable = false

    /**
     * TODO: This is temporary to use it in the test cases.
     */
    var httpCallbackURL: String? = null

    var vulnerabilityClasses: MutableList<VulnerabilityClass> = mutableListOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses.add(vulnerabilityClass)
    }
}
