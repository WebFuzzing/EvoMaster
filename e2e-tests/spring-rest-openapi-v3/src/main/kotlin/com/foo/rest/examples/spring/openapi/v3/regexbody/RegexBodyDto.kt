package com.foo.rest.examples.spring.openapi.v3.regexbody

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

open class RegexBodyDto {

    @get:NotNull
    @get:Pattern(regexp = "^moo+$")
    var foo : String? = null
}