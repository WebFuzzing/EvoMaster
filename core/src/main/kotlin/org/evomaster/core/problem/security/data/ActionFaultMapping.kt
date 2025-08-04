package org.evomaster.core.problem.security.data

import com.webfuzzing.commons.faults.FaultCategory

class ActionMapping (
    val name: String,
) {

    /**
     * Key holds the parameter name, and the value holds the related [InputMapping].
     */
    var params: Map<String, InputMapping> = mutableMapOf()

    var isVulnerable = false

    var isExploitable = false

    /**
     * TODO: This is temporary to use it in the test cases.
     */
    var httpCallbackURL: String? = null

    /**
     * Holds potential security faults for the [Action].
     * Key contains the faults related to security, and value marks the exploitability
     * using a [Boolean].
     */
    var securityFaults: MutableMap<FaultCategory, Boolean> = mutableMapOf()

    fun addSecurityFaultCategory(faultCategory: FaultCategory) {
        securityFaults[faultCategory] = false
    }
}
