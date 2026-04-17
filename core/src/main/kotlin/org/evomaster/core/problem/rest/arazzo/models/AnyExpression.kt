package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

sealed class AnyExpression {
    data class Constant(val data: JsonNode) : AnyExpression()

    data class Expression(val expression: RuntimeExpression) : AnyExpression()
}