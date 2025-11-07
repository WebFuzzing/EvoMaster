package com.foo.rest.examples.spring.openapi.v3.swagger

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

class PersonDto {

    @NotNull
    @Schema(name = "name", example = "John", required = true, description = "Name to be greeted")
    var name : String? = null

    @Schema(name = "age", example = "29", required = false, description = "Age of the person")
    var age: Int = 0
}
