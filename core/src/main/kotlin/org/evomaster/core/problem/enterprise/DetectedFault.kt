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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectedFault

        if (category != other.category) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }


}