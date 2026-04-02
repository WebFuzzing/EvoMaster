package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class Workflow(
    val workflowId: String,
    val summary: String?,
    val description: String?,
    val inputs: String?,
    val dependsOn: List<String>?,
    val steps: List<Step>,
    val successActions: List<JsonNode>?,
    val failureActions: List<JsonNode>?,
    val outputs: Map<String, String>?,
    val parameters: List<JsonNode>?
) {
}