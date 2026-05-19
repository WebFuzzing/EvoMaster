package com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.xml

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.nonidempotentput.xml.HttpNonIdempotentPutXMLApplication


class HttpNonIdempotentPutXMLController: SpringController(HttpNonIdempotentPutXMLApplication::class.java){

    override fun resetStateOfSUT() {
        HttpNonIdempotentPutXMLApplication.reset()
    }
}