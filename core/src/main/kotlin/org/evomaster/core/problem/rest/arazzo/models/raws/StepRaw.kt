package org.evomaster.core.problem.rest.arazzo.models.raws

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.FailureReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.ParameterReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.RuntimeExpressionDeserializer
import org.evomaster.core.problem.rest.arazzo.deserializer.SuccessReusableDeserializer
import org.evomaster.core.problem.rest.arazzo.models.Criterion
import org.evomaster.core.problem.rest.arazzo.models.FailureReusable
import org.evomaster.core.problem.rest.arazzo.models.ParameterReusable
import org.evomaster.core.problem.rest.arazzo.models.RequestBody
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression
import org.evomaster.core.problem.rest.arazzo.models.SuccessReusable
import org.evomaster.core.problem.rest.arazzo.models.commons.StepCommon

class StepRaw(
    override val description: String?,
    override val stepId: String,
    override val operationId: String?,
    override val operationPath: String?,
    override val workflowId: String?,
    override val requestBody: RequestBody?,
    override val successCriteria: List<Criterion>?,
    
    @JsonDeserialize(contentUsing = RuntimeExpressionDeserializer::class)
    override val outputs: Map<String, RuntimeExpression>?,

    @JsonDeserialize(contentUsing = ParameterReusableDeserializer::class)
    val parameters: List<ParameterReusable>?,

    @JsonDeserialize(contentUsing = SuccessReusableDeserializer::class)
    val onSuccess: List<SuccessReusable>?,

    @JsonDeserialize(contentUsing = FailureReusableDeserializer::class)
    val onFailure: List<FailureReusable>?
) : StepCommon