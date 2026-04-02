package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class Step(
    val description: String?,
    val stepId: String,
    val operationId: String?,
    val operationPath: String?,
    val workflowId: String?,
    val parameters: List<JsonNode>?,
    val requestBody: RequestBody?,
    val successCriteria: List<Criterion>?,
    val onSuccess: List<JsonNode>?,
    val onFailure: List<JsonNode>?,
    val outputs: Map<String, String>?
) {
}