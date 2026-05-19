package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.json

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpNonIdempotentPutController: SpringController(HttpNonIdempotentPutApplication::class.java){

    override fun resetStateOfSUT() {
        HttpNonIdempotentPutApplication.reset()
    }
}