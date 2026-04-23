package com.foo.rest.examples.spring.openapi.v3.httporacle.repeatedput

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpOracleRepeatedPutController: SpringController(HttpOracleRepeatedPutApplication::class.java){

    override fun resetStateOfSUT() {
        HttpOracleRepeatedPutApplication.reset()
    }
}