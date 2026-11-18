package org.evomaster.core.problem.rest.arazzo

/**
 * The input and output expressions for each AraazzoStep are stored.
 */
data class ArazzoStepSpecs (
    val workflowId: String,
    val stepId: String,
    val parameterExpressions: Map<String, String>,
    val outputExpressions: Map<String, String> = emptyMap(),
)