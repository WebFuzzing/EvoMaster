package org.evomaster.core.problem.security.data

import com.webfuzzing.commons.faults.FaultCategory

class InputFaultMapping (
    val name: String,
    val description: String?
) {

    /**
     * Holds potential vulnerability class for the [Param].
     */
    var securityFaults: MutableList<FaultCategory> = mutableListOf()

    fun addSecurityFaultCategory(faultCategory: FaultCategory) {
        securityFaults.add(faultCategory)
    }

}
