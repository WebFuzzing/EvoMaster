package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class PayloadReplacement(
    val target: String,
    val value: JsonNode
) {
}