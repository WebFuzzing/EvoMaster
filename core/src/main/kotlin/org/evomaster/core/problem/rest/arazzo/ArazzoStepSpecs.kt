package org.evomaster.core.problem.rest.arazzo

/**
 * The inputs and outputs for each ArazzoStep are stored.
 */
data class ArazzoStepSpecs (
    val workflowId: String,
    val frameId: Int,
    val stepId: String,
    val parameters: List<ArazzoParameterSpec>,
    val outputs: Map<String, String> = emptyMap(),
    val workflowInputs: List<ArazzoParameterSpec> = emptyList(),
    val closes: List<FrameClose> = emptyList(),
    val requestBody: String? = null,
)

/**
 * Notice that a workflow ends right after the action that carries it.
 * It holds what the runtime needs to compute the values produced by that workflow.
 */
data class FrameClose(
    val workflowOutputs: Map<String, String>,
    val parentStepId: String?,
    val parentStepOutputs: Map<String, String>,
)

/**
 * Parameter for each steps
 */
data class ArazzoParameterSpec(
    val name: String,
    val location: String?,
    val expression: String,
)
