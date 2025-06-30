package org.evomaster.core.problem.enterprise

import com.webfuzzing.commons.faults.FaultCategory

/**
 * Note: should be Immutable
 */
class DetectedFault(
    val category: FaultCategory,
    /**
     * For a REST API, this would be the endpoint VERB:PATH
     */
    val operationId: String,
    /**
     * For same operation and save fault type, we could detect more than 1 fault.
     * We distinguish them based on the context, ie, a discriminating string.
     * This does not apply to all kinds of faults, so it is an optional field.
     * For example, in WB for HTTP 500, the discriminating context could be based on last executed
     * line in the SUT.
     */
    _context: String?
) {

    //otherwise issues when printing in comments
    val context = _context?.replace('\n',' ')

    private val _toString = "Detected ${category.label} in ${operationId}. Context: $context}"

    override fun toString(): String {
        return _toString
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectedFault

        if (category != other.category) return false
        if (operationId != other.operationId) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + operationId.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }


}