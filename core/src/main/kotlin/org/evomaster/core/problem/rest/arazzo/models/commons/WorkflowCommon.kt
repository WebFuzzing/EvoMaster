package org.evomaster.core.problem.rest.arazzo.models.commons

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.arazzo.models.RuntimeExpression

interface WorkflowCommon {
    val workflowId: String
    val summary: String?
    val description: String?
    val dependsOn: List<String>?
    val outputs: Map<String, RuntimeExpression>?
}