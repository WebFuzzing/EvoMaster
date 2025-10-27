package com.foo.rest.examples.spring.openapi.v3.wiremock.jsonarray

class WmJsonCycleDto {

    var y : Int? = null

    var referToSelf : WmJsonCycleDto? = null
}