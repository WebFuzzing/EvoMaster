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
    _context: String?,
    /**
     * In some cases, there could be extra useful information.
     * For example, in a schema oracle, if a field is invalid, we can tell which value is invalid.
     * however, this might depend on the content of the test case (eg "20-20-20" is not a valid date).
     * And so, it CANNOT be used to uniquely identify a fault type.
     * The same fault type with same context could appear in different tests having different localMessage
     */
    val localMessage: String? = null
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