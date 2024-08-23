package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

/**
 * Note: should be Immutable
 */
class DetectedFault(
    val category: FaultCategory,
    val context: String
) {

    override fun toString(): String {
        return "Detected ${category.label}. $context"
    }
}