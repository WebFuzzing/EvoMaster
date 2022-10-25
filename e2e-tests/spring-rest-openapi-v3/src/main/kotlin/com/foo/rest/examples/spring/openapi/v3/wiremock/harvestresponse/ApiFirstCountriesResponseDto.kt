package com.foo.rest.examples.spring.openapi.v3.wiremock.harvestresponse

class ApiFirstCountriesResponseDto {

    var status : String? = null

    var `status-code` : Int? = null

    var version : String? = null

    var access : String? = null

    /**
     * this needs to be updated later once support map
     */
    var data : Map<String, CountryDto> ?= null
}