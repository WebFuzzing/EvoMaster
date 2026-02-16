package com.foo.rest.examples.spring.openapi.v3.dtoreflectiveassert

import com.fasterxml.jackson.annotation.JsonAnySetter

class AdditionalPropsNoRootDto() {

    val additional: MutableMap<String, AdditionalPropsDto> = mutableMapOf()

    @JsonAnySetter
    fun addAdditional(key: String, value: AdditionalPropsDto) {
        additional[key] = value
    }

}
