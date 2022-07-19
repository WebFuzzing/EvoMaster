package org.evomaster.core.problem.external.service

import java.util.UUID

/**
 * Represent an external service call made to a WireMock
 * instance from a SUT.
 *
 * TODO: Properties have to extended further based on the need
 */
class ExternalServiceRequest(
    private val id: UUID,
    private val method: String,
    private val url: String,
    private val wasMatched: Boolean,
) {

    fun getID(): String {
        return id.toString()
    }

    /**
     * return the URL
     *
     * TODO: for now represents the absolute URL need to add more
     *  information in future
     */
    fun getURL(): String {
        return url
    }

    fun getMethod(): String {
        return method
    }

    fun getWasMatched(): Boolean {
        return wasMatched
    }
}