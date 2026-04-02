package org.evomaster.core.problem.rest.arazzo.models

class SuccessAction(
    val name: String,
    val type: String,
    val workflowId: String?,
    val stepId: String?,
    val criteria: List<Criterion>
) {
}