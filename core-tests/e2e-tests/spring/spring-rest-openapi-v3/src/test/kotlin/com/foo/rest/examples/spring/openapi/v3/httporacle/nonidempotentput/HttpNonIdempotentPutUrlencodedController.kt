package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.urlencoded

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.xml.HttpNonIdempotentPutXMLApplication


class HttpNonIdempotentPutUrlencodedController: SpringController(HttpNonIdempotentPutUrlEncodedApplication::class.java){

    override fun resetStateOfSUT() {
        HttpNonIdempotentPutUrlEncodedApplication.reset()
    }
}