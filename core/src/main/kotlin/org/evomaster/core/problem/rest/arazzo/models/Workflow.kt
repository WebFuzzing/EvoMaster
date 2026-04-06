package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.FailureReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.ParameterReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.RuntimeExpressionDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.SuccessReusableDeserializer

class Workflow(
    val workflowId: String,
    val summary: String?,
    val description: String?,
    val inputs: JsonNode?,
    val dependsOn: List<String>?,
    val steps: List<Step>,
    @JsonDeserialize(contentUsing = SuccessReusableDeserializer::class)
    val successActions: List<SuccessReusable>?,
    @JsonDeserialize(contentUsing = FailureReusableDeserializer::class)
    val failureActions: List<FailureReusable>?,
    @JsonDeserialize(contentUsing = RuntimeExpressionDeserializer::class)
    val outputs: Map<String, RuntimeExpression>?,
    @JsonDeserialize(contentUsing = ParameterReusableDeserializer::class)
    val parameters: List<ParameterReusable>?
) {
}