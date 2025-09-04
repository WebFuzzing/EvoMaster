package org.evomaster.core.problem.security.data

import com.webfuzzing.commons.faults.FaultCategory

/**
 * [ActionFaultMapping] represents the [Action] and related [Param] with security faults.
 * This used in [SSRFAnalyser].
 */
class ActionFaultMapping(
    val name: String,
) {

    /**
     * Key holds the parameter name, and the value holds the related [InputFaultMapping].
     */
    var params: Map<String, InputFaultMapping> = mutableMapOf()

    var isVulnerable = false

    /**
     * Holds potential security faults for the [Action].
     */
    var securityFaults: MutableList<FaultCategory> = mutableListOf()

    fun addSecurityFaultCategory(faultCategory: FaultCategory) {
        securityFaults.add(faultCategory)
    }

    fun getVulnerableParameterName(): String? {
        val params = params.filter { it.value.hasSSRFFaults() }
        if (params.isNotEmpty()) {
            return params.keys.first()
        }
        return null
    }

}
