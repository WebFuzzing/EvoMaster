package org.evomaster.core.problem.external.service

import java.util.UUID

class ExternalServiceRequest(
    private val id: UUID,
    private val absoluteUrl: String
) {

    fun getID(): String {
        return id.toString()
    }

    fun getURL(): String {
        return absoluteUrl
    }
}