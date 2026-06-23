package com.foo.rest.examples.spring.openapi.v3.httporacle.misleadingcreateput

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpMisleadingCreatePutController: SpringController(MisleadingCreatePutApplication::class.java){

    override fun resetStateOfSUT() {
        MisleadingCreatePutApplication.reset()
    }
}
