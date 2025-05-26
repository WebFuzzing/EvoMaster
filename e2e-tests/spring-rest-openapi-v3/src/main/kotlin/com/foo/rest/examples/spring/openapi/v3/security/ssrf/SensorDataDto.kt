package com.foo.rest.examples.spring.openapi.v3.security.ssrf

import io.swagger.v3.oas.annotations.media.Schema

class SensorDataDto {
    @Schema(name = "temp", example = "30", required = true, description = "Temperature from the sensor")
    val temp: Float = 0.0f
}
