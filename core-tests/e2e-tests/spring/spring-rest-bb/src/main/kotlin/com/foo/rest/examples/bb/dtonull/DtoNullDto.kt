package com.foo.rest.examples.bb.dtonull

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Optional

@JsonInclude(JsonInclude.Include.NON_NULL)
open class DtoNullDto {

    @field:JsonProperty("x")
    var x: Optional<Int>? = null
}