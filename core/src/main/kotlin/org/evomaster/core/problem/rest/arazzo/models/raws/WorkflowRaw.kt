package org.evomaster.core.problem.rest.arazzo.models.raws

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.arazzo.deserializer.FailureReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.ParameterReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.RuntimeExpressionDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.SuccessReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.models.FailureReusable
import org.evomaster.core.problem.rest.arazzo.models.ParameterReusable
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression
import org.evomaster.core.problem.rest.arazzo.models.Step
import org.evomaster.core.problem.rest.arazzo.models.SuccessReusable
import org.evomaster.core.problem.rest.arazzo.models.commons.WorkflowCommon

class WorkflowRaw(
    override val workflowId: String,
    override val summary: String?,
    override val description: String?,
    override val dependsOn: List<String>?,
    val inputs: Schema<*>?,
    val steps: List<StepRaw>,

    @JsonDeserialize(contentUsing = SuccessReusableDeserializer::class)
    val successActions: List<SuccessReusable>?,

    @JsonDeserialize(contentUsing = FailureReusableDeserializer::class)
    val failureActions: List<FailureReusable>?,

    @JsonDeserialize(contentUsing = RuntimeExpressionDeserializer::class)
    override val outputs: Map<String, RuntimeExpression>?,

    @JsonDeserialize(contentUsing = ParameterReusableDeserializer::class)
    val parameters: List<ParameterReusable>?
) : WorkflowCommon