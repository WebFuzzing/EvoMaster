package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class Components(
    val inputs: Map<String, JsonNode>?,
    val parameters: Map<String, Parameter>?,
    val successActions: Map<String, SuccessAction>?,
    val failureActions: Map<String, FailureAction>?
) {
}