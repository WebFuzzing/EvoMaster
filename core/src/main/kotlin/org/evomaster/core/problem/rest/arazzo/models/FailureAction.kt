package org.evomaster.core.problem.rest.arazzo.models

class FailureAction(
    val name: String,
    val type: String,
    val workflowId: String?,
    val stepId: String?,
    val retryAfter: Number?,
    val retryLimit: Integer?,
    val criteria: Criterion
) {
}