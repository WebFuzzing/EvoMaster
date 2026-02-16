package com.foo.rest.examples.spring.openapi.v3.dtoreflectiveassert

import com.fasterxml.jackson.annotation.JsonAnySetter

class AdditionalPropsRefDto(val stringProp: String) {

    val additional: MutableMap<String, ChildSchemaDto> = mutableMapOf()

    @JsonAnySetter
    fun addAdditional(key: String, value: ChildSchemaDto) {
        additional[key] = value
    }

}
