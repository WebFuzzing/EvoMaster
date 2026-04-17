package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.FailureReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.ParameterReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.SuccessReusableDeserializer

class Step(
    val description: String?,
    val stepId: String,
    val operationId: String?,
    val operationPath: String?,
    val workflowId: String?,
    @JsonDeserialize(contentUsing = ParameterReusableDeserializer::class)
    val parameters: List<ParameterReusable>?,
    val requestBody: RequestBody?,
    val successCriteria: List<Criterion>?,
    @JsonDeserialize(contentUsing = SuccessReusableDeserializer::class)
    val onSuccess: List<SuccessReusable>?,
    @JsonDeserialize(contentUsing = FailureReusableDeserializer::class)
    val onFailure: List<FailureReusable>?,
    val outputs: Map<String, String>?
) {
}