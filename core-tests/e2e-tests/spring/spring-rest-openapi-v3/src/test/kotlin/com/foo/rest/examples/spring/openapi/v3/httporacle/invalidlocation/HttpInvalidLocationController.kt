package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpInvalidLocationController: SpringController(HttpOracleRepeatedPutApplication::class.java){

    override fun resetStateOfSUT() {
        HttpOracleRepeatedPutApplication.reset()
    }
}
