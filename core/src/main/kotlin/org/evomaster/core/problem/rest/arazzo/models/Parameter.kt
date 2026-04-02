package org.evomaster.core.problem.rest.arazzo.models

import com.fasterxml.jackson.annotation.JsonProperty

class Parameter(
    val name: String,
    @JsonProperty("in")
    val location: String?,
    val value: String
) {
}