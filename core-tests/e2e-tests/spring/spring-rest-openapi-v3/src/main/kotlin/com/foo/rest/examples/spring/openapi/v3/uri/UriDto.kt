package com.foo.rest.examples.spring.openapi.v3.uri

import javax.validation.constraints.NotNull

class UriDto {

    @get:NotNull
    var x : String? = null
}