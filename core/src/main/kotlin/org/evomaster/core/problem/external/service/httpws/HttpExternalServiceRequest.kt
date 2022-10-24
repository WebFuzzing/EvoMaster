package org.evomaster.core.problem.external.service.httpws

import java.util.UUID

/**
 * Represent an external service call made to a WireMock
 * instance from a SUT.
 *
 * TODO: Properties have to extended further based on the need
 */
class HttpExternalServiceRequest(
    /**
     * Most likely the UUID created by the respective WireMock
     * while capturing the request.
     */
    val id: UUID,
    val method: String,
    val url: String,
    val absoluteURL: String,
    val wasMatched: Boolean,
    /**
     * refers to the WireMock instance which received the request.
     */
    val wireMockSignature: String
) {

    fun getId() : String {
        return id.toString()
    }

    /**
     * get description of this an HTTP request to external service
     */
    fun getDescription() = "$method:$absoluteURL"
}