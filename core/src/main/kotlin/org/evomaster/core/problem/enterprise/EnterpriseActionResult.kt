package org.evomaster.core.problem.enterprise

import com.google.common.annotations.VisibleForTesting
import org.evomaster.core.search.action.ActionResult

abstract class EnterpriseActionResult : ActionResult{

    constructor(sourceLocalId: String, stopping: Boolean = false) : super(sourceLocalId, stopping)

    @VisibleForTesting
    internal constructor(other: EnterpriseActionResult) : super(other){
        //recall that DetectedFault is immutable
        detectedFaults.addAll(other.detectedFaults)
    }

    private val detectedFaults = mutableListOf<DetectedFault>()

    fun addFault(fault: DetectedFault){
        detectedFaults.add(fault)
    }

    fun getFaults() : List<DetectedFault>{
        return detectedFaults
    }
}