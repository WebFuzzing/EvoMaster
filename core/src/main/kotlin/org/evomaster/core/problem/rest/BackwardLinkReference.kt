package org.evomaster.core.problem.rest

/**
 * When there is a link from A (the source) to B, this latter needs to have info about such link.
 * This object defines it.
 * Note that several links could be defined in A.
 */
class BackwardLinkReference(
    val sourceActionId: String,
    val sourceLinkId: String
) {

    val statusCode = sourceLinkId.substringBefore(":").toInt()
}