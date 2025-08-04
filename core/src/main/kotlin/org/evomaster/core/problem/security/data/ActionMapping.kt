package org.evomaster.core.problem.security.data

import org.evomaster.core.problem.security.VulnerabilityClass

class ActionMapping (
    val name: String,
) {

    var params: Map<String, InputMapping> = mutableMapOf()

    var isVulnerable = false

    var isExploitable = false

    /**
     * TODO: This is temporary to use it in the test cases.
     */
    var httpCallbackURL: String? = null

    /**
     * Holds potential vulnerability class for the [Action].
     * Key contains the vulnerability class, and value marks the exploitability
     * using a [Boolean].
     */
    var vulnerabilityClasses: MutableMap<VulnerabilityClass, Boolean> = mutableMapOf()

    fun addVulnerabilityClass(vulnerabilityClass: VulnerabilityClass) {
        vulnerabilityClasses[vulnerabilityClass] = false
    }
}
