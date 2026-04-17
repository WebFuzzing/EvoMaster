package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class RequestBody(
    val contentType: String?,
    val payload: JsonNode?,
    val replacements: List<PayloadReplacement>?
) {
}