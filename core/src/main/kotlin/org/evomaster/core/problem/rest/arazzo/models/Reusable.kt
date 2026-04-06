package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.RuntimeExpressionDeserializer

class Reusable(
    @JsonDeserialize(using = RuntimeExpressionDeserializer::class)
    val reference: RuntimeExpression,
    val value: String?
) {
}