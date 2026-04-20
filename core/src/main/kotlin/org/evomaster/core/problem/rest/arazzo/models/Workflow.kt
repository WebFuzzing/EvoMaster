package org.evomaster.core.problem.rest.arazzo.models

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.arazzo.models.commons.WorkflowCommon

class Workflow(
    common: WorkflowCommon,
    val  inputs: Schema<*>?,
    val steps: List<Step>,
    val successActions: List<SuccessAction>?,
    val failureActions: List<FailureAction>?,
    val parameters: List<Parameter>?
) : WorkflowCommon by common