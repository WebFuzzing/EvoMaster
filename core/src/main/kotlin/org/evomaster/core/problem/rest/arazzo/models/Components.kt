package org.evomaster.core.problem.rest.arazzo.models

import io.swagger.v3.oas.models.media.Schema

class Components(
    val inputs: Map<String, Schema<*>>?,
    val parameters: Map<String, Parameter>?,
    val successActions: Map<String, SuccessAction>?,
    val failureActions: Map<String, FailureAction>?
) {
}