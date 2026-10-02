package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidlocationfp

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpInvalidLocationFPController: SpringController(InvalidLocationFPApplication::class.java){

    override fun resetStateOfSUT() {
        InvalidLocationFPApplication.reset()
    }
}
