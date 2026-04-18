package org.evomaster.core.problem.rest.arazzo.mapper

import org.evomaster.core.problem.rest.arazzo.models.ArazzoSpecifications
import org.evomaster.core.problem.rest.arazzo.models.Step
import org.evomaster.core.problem.rest.arazzo.models.Workflow
import org.evomaster.core.problem.rest.arazzo.models.raws.ArazzoSpecificationsRaw
import org.evomaster.core.problem.rest.arazzo.models.raws.StepRaw
import org.evomaster.core.problem.rest.arazzo.models.raws.WorkflowRaw
import org.evomaster.core.problem.rest.arazzo.resolver.ArazzoReferenceResolver

class ArazzoMapper(
    val resolver: ArazzoReferenceResolver
) {

    fun toDomain(raw: ArazzoSpecificationsRaw) : ArazzoSpecifications {
        return ArazzoSpecifications(
            common = raw,
            workflows = raw.workflows.map { toDomain(it) }
        )
    }

    fun toDomain(raw: WorkflowRaw) : Workflow {
        return Workflow(
            common = raw,
            steps = raw.steps.map { toDomain(it) },
            successActions = resolver.resolveSuccessReusable(raw.successActions),
            failureActions = resolver.resolveFailureReusable(raw.failureActions),
            parameters = resolver.resolveParametersReusable(raw.parameters)
        )
    }

    fun toDomain(raw: StepRaw) : Step {
        return Step(
            common = raw,
            parameters = resolver.resolveParametersReusable(raw.parameters),
            onSuccess = resolver.resolveSuccessReusable(raw.onSuccess),
            onFailure = resolver.resolveFailureReusable(raw.onFailure)
        )
    }

}