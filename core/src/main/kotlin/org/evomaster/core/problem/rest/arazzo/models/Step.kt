package org.evomaster.core.problem.rest.arazzo.models

import org.evomaster.core.problem.rest.arazzo.models.commons.StepCommon

class Step(
    common: StepCommon,
    val parameters: List<Parameter>?,
    val onSuccess: List<SuccessAction>?,
    val onFailure: List<FailureAction>?,
) : StepCommon by common