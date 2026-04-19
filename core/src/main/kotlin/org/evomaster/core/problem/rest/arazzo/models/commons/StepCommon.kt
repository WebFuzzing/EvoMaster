package org.evomaster.core.problem.rest.arazzo.models.commons

import org.evomaster.core.problem.rest.arazzo.models.Criterion
import org.evomaster.core.problem.rest.arazzo.models.RequestBody
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression

interface StepCommon {
    val description: String?
    val stepId: String
    val operationId: String?
    val operationPath: String?
    val workflowId: String?
    val requestBody: RequestBody?
    val successCriteria: List<Criterion>?
    val outputs: Map<String, RuntimeExpression>?
}