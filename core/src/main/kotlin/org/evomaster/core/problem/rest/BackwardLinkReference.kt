package org.evomaster.core.problem.rest

/**
 * When there is a link from A (the source) to B, this latter needs to have info about such link.
 * This object defines it.
 * Note that several links could be defined in A.
 */
class BackwardLinkReference(
    val sourceActionId: String,
    val sourceLinkId: String,

    /**
     * In current EM architecture, local ids would unfortunately not be available yet when this object is created.
     * Furthermore, the update might fail (eg input not satisfying a constraint).
     * As such, we use this variable to infer if a link is usable
     */
    var actualSourceActionLocalId: String? = null
) {

    val statusCode = sourceLinkId.substringBefore(":").toInt()

    fun isInUse() = actualSourceActionLocalId != null

    fun copy() = BackwardLinkReference(sourceActionId, sourceLinkId, actualSourceActionLocalId)
}