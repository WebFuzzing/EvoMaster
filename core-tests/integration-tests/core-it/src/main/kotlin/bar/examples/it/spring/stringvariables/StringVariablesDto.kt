package bar.examples.it.spring.stringvariables

import bar.examples.it.spring.body.BodyOtherDto
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

class StringVariablesDto(

    var foo: String? = null,

    @get:JsonProperty(required = true)
    var bar: String? = null,

    var nested: StringVariablesDto? = null
)