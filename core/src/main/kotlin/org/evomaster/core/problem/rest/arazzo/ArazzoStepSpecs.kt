package org.evomaster.core.problem.rest.arazzo

/**
 * The inputs and outputs for each ArazzoStep are stored.
 */
data class ArazzoStepSpecs (
    val workflowId: String,
    val stepId: String,
    val parameters: List<ArazzoParameterSpec>,
    val outputs: Map<String, String> = emptyMap(),
    val workflowInputs: List<ArazzoParameterSpec> = emptyList(),
)

/**
 * Parameter for each steps
 */
data class ArazzoParameterSpec(
    val name: String,
    val location: String?,
    val expression: String,
)