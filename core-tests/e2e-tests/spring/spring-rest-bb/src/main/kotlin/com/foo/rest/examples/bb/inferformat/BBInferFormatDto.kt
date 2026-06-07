package com.foo.rest.examples.bb.inferformat

import io.swagger.v3.oas.annotations.media.Schema

class BBInferFormatDto(

    @field:Schema(description = "This is a UUID field.")
    var foo: String? = null,

    @field:Schema(description = "This is a field representing a date in ISO 8601 format.")
    var bar: String? = null,
)