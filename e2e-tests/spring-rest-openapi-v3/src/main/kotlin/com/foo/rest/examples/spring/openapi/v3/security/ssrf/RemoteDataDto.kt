package com.foo.rest.examples.spring.openapi.v3.security.ssrf

import io.swagger.v3.oas.annotations.media.Schema

class RemoteDataDto {

    @Schema(name = "sensorUrl", example = "http://example.com/data/json", required = true, description = "Remote sensor url to fetch data")
    var sensorUrl: String? = null
}
