package org.evomaster.core.problem.security.data

import com.webfuzzing.commons.faults.FaultCategory

class InputMapping (
    val name: String,
    val description: String?
) {

    /**
     * Holds potential vulnerability class for the [Param].
     * Key contains the security related faults, and value marks the exploitability
     * using a [Boolean].
     */
    var securityFaults: MutableList<FaultCategory> = mutableListOf()

    fun addSecurityFaultCategory(faultCategory: FaultCategory) {
        securityFaults.add(faultCategory)
    }

}
