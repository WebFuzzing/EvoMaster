package org.evomaster.core.problem.external.service

import java.util.UUID

/**
 * Represent an external service call made to a WireMock
 * instance from a SUT.
 *
 * TODO: Properties have to extended further based on the need
 */
class ExternalServiceRequest(
    val id: UUID,
    val method: String,
    val url: String,
    val absoluteURL: String,
    val wasMatched: Boolean,
) {

    fun getSignature(): String {
        return method.plus(absoluteURL)
    }
}