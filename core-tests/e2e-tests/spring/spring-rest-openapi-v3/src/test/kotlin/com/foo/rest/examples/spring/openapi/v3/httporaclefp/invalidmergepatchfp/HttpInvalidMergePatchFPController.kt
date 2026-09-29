package com.foo.rest.examples.spring.openapi.v3.httporaclefp.invalidmergepatchfp

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpInvalidMergePatchFPController: SpringController(InvalidMergePatchFPApplication::class.java){

    override fun resetStateOfSUT() {
        InvalidMergePatchFPApplication.reset()
    }
}
