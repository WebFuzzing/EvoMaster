package com.foo.rest.examples.spring.openapi.v3.httporacle.delete

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpOracleDeleteController: SpringController(HttpOracleDeleteApplication::class.java){

    override fun resetStateOfSUT() {
        HttpOracleDeleteApplication.reset()
    }
}