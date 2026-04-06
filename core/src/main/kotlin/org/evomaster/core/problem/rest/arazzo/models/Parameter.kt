package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.AnyExpressionDeserializer

class Parameter(
    val name: String,
    @JsonProperty("in")
    val location: String?,
    @JsonDeserialize(using = AnyExpressionDeserializer::class)
    val value: AnyExpression
) {
}