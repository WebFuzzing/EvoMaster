package org.evomaster.core.problem.rest.arazzo.models

import org.evomaster.core.problem.rest.arazzo.models.commons.WorkflowCommon

class Workflow(
    common: WorkflowCommon,
    val steps: List<Step>,
    val successActions: List<SuccessAction>?,
    val failureActions: List<FailureAction>?,
    val parameters: List<Parameter>?
) : WorkflowCommon by common