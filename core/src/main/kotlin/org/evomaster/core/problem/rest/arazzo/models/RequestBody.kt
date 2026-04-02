package org.evomaster.core.problem.rest.arazzo.models

class RequestBody(
    val contentType: String?,
    val payload: Any?,
    val replacements: List<PayloadReplacement>?
) {
}