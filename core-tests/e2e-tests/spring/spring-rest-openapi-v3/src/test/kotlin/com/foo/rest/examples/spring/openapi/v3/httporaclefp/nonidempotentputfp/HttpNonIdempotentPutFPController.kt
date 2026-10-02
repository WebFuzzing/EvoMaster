package com.foo.rest.examples.spring.openapi.v3.httporaclefp.nonidempotentputfp

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpNonIdempotentPutFPController: SpringController(NonIdempotentPutFPApplication::class.java){

    override fun resetStateOfSUT() {
        NonIdempotentPutFPApplication.reset()
    }
}