package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.array

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpNonIdempotentPutArrayController: SpringController(HttpNonIdempotentPutArrayApplication::class.java){

    override fun resetStateOfSUT() {
        HttpNonIdempotentPutArrayApplication.reset()
    }
}
