package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.databind.JsonNode

class Criterion(
    val context: String?,
    val condition: String,
    val type: JsonNode?
) {

}