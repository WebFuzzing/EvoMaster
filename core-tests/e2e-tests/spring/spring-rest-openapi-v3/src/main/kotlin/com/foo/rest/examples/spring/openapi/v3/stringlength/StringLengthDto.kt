package com.foo.rest.examples.spring.openapi.v3.stringlength

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

open class StringLengthDto {

    @get:NotNull
    var foo : String? = null
}