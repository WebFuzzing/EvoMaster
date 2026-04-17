package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.evomaster.core.problem.rest.arazzo.deserializer.CriterionTypeDeserializer

class Criterion(
    val context: RuntimeExpression?,
    val condition: String,
    @JsonDeserialize(using = CriterionTypeDeserializer::class)
    val type: CriterionType?
) {

}